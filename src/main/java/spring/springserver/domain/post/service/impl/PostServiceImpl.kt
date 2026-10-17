package spring.springserver.domain.post.service.impl

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.file.service.FileService
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.service.MemberService
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.service.PostService
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.config.PostRepeatRateProperties
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.jwt.MemberDetails
import spring.springserver.global.util.PageableSortResolver
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class PostServiceImpl(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val fileService: FileService,
    private val jobCategoryService: JobCategoryService,
    private val profileService: ProfileService,
    private val memberService: MemberService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val postRepeatRateProperties: PostRepeatRateProperties
): PostService {

    override fun createPost(
        createPostRequest: CreatePostRequest
    ): PostResponse {

        val member = getCurrentMember()

        memberService.ensurePhoneVerified(member = member)

        val post = createPostRequest.toEntity(
            member,
            resolveCategory(createPostRequest.categoryId, null)
        )

        requireAttachments(normalizeFileUrls(createPostRequest.fileUrls.orEmpty()))
            .forEach { fileUrl -> post.addAttachment(fileUrl) }

        return toResponse(postRepository.save(post))
    }

    /**
     * 조회수를 올려야 하므로 readOnly로 두지 않는다.
     * readOnly면 flush가 일어나지 않아 증가분이 저장되지 않는다.
     */
    override fun viewPost(
        id: Long,
        clientIp: String,
        userAgent: String
    ): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        /**
         * 응답을 먼저 만든다. 조회수 UPDATE가 `clearAutomatically`로 영속성 컨텍스트를
         * 비우므로, 지연 로딩이 남아 있으면 그 뒤에는 초기화할 수 없다.
         */
        val postResponse = toResponse(post)

        if (!shouldCountView(
                post,
                clientIp,
                userAgent
            )) {

            return postResponse
        }

        /**
         * 증가는 `viewCount = viewCount + 1` UPDATE로 원자적으로 처리한다.
         * 엔티티에서 읽고 더하면 서로 다른 회원의 동시 조회가 서로를 덮어쓴다.
         */
        postRepository.incrementViewCount(id)

        return postResponse.copy(viewCount = postResponse.viewCount + 1)
    }

    /**
     * 로그인 상태면 회원 id 기준으로 하루 1회만 센다. 작성자 본인 조회는 제외한다.
     * 비로그인은 IP + User-Agent 해시로 5분 내 재조회를 걸러낸다. 전시용 임시 완화이며,
     * 전시 종료 후 쿠키 기반 anonymous UUID로 교체 예정.
     *
     * Redis가 죽어 있으면 조회수를 세지 않는 쪽으로 넘어간다.
     * 상세 조회 자체가 실패하는 것보다 집계가 조금 비는 편이 낫다.
     */
    private fun shouldCountView(
        post: Post,
        clientIp: String,
        userAgent: String
    ): Boolean {

        val memberId = currentMemberIdOrNull()

        if (memberId != null && memberId == post.member.getId()) {

            return false
        }

        val key = if (memberId != null) {

            "$VIEW_COUNT_KEY_PREFIX${post.getId()}:$memberId"
        } else {

            "$VIEW_COUNT_ANON_KEY_PREFIX${post.getId()}:$clientIp:${hashUserAgent(userAgent)}"
        }

        val ttl = if (memberId != null) VIEW_COUNT_TTL else VIEW_COUNT_ANON_TTL

        return runCatching {

            redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl)
                ?: false
        }.getOrDefault(false)
    }

    /**
     * User-Agent는 길이가 들쭉날쭉하므로 Redis 키 길이를 안정화하려고 짧게 해시한다.
     */
    private fun hashUserAgent(
        userAgent: String
    ): String {

        if (userAgent.isBlank()) {

            return "none"
        }

        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(userAgent.toByteArray())

        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * 인증 주체에 이미 회원 id가 실려 있으면 그대로 쓴다.
     * 소셜 로그인처럼 주체 타입이 다른 경우에만 username으로 한 번 찾는다.
     */
    private fun currentMemberIdOrNull(): Long? {

        val authentication = SecurityContextHolder.getContext().authentication
            ?: return null

        return (authentication.principal as? MemberDetails)?.getId()
            ?: authentication.name
                ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
                ?.let { username -> memberRepository.findByUsername(username)?.getId() }
    }

    override fun viewAllPosts(
        pageable: Pageable
    ): Page<PostResponse> {

        if (isRepeatRateSort(pageable)) {

            return toResponses(
                postRepository.findAllLivingOrderByRepeatRate(
                    postRepeatRateProperties.minBuyers,
                    PageRequest.of(pageable.pageNumber, pageable.pageSize, REPEAT_RATE_SORT_HINT)
                )
            )
        }

        return toResponses(
            postRepository.findAllByIsDeletedFalse(resolveSort(pageable))
        )
    }

    override fun searchPosts(
        title: String?,
        categoryId: Long?,
        pageable: Pageable
    ): Page<PostResponse> {

        val normalizedTitle = title?.trim().orEmpty()

        if (isRepeatRateSort(pageable)) {

            val pageRequest = PageRequest.of(pageable.pageNumber, pageable.pageSize, REPEAT_RATE_SORT_HINT)

            if (categoryId == null) {

                return toResponses(
                    postRepository.searchPostsByTitleOrderByRepeatRate(
                        normalizedTitle,
                        postRepeatRateProperties.minBuyers,
                        pageRequest
                    )
                )
            }

            return toResponses(
                postRepository.searchPostsByTitleAndCategoryIdsOrderByRepeatRate(
                    normalizedTitle,
                    jobCategoryService.getCategoryIdsIncludingDescendants(categoryId),
                    postRepeatRateProperties.minBuyers,
                    pageRequest
                )
            )
        }

        val sorted = resolveSort(pageable)

        if (categoryId == null) {

            return toResponses(
                postRepository.searchPostsByTitle(
                    normalizedTitle,
                    sorted
                )
            )
        }

        return toResponses(
            postRepository.searchPostsByTitleAndCategoryIds(
                normalizedTitle,
                jobCategoryService.getCategoryIdsIncludingDescendants(categoryId),
                sorted
            )
        )
    }

    /**
     * 재구매율은 표본 보정이 들어가므로 Pageable.sort 로는 표현할 수 없고,
     * CASE WHEN 이 박힌 전용 @Query 로 라우팅한다. 1차 정렬이 repeatRate 일 때만 전용 경로로 간다.
     */
    private fun isRepeatRateSort(
        pageable: Pageable
    ): Boolean {

        return pageable.sort
            .toList()
            .firstOrNull()
            ?.property == "repeatRate"
    }

    private fun resolveSort(
        pageable: Pageable
    ): Pageable {

        return PageableSortResolver.resolve(
            pageable,
            POST_SORT_ALLOWED,
            POST_SORT_DEFAULT,
            POST_SORT_TIEBREAKER
        )
    }

    override fun updatePost(
        updatePostRequest: UpdatePostRequest
    ): PostResponse {

        val post = postRepository.findPostById(updatePostRequest.id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.title = updatePostRequest.title

        post.content = updatePostRequest.content

        post.category = resolveCategory(updatePostRequest.categoryId, post.category)

        updatePostRequest.fileUrls
            ?.let { fileUrls ->

                replaceAttachments(
                    post,
                    requireAttachments(normalizeFileUrls(fileUrls))
                )
            }

        post.preUpdate()

        post.isEdited = true

        return toResponse(post)
    }

    override fun deletePost(
        id: Long
    ): DeletedPostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.isDeleted = true
        post.deletedAt = LocalDateTime.now()

        return DeletedPostResponse.of("삭제되었습니다")
    }

    private fun toResponse(
        post: Post
    ): PostResponse {

        val memberId = post.member.getId()

        return PostResponse.of(
            post,
            memberId?.let { id -> profileService.getImageUrlByMemberId(id) }
        )
    }

    /**
     * 목록은 회원별 프로필 이미지를 한 번에 조회해 게시글마다 조회하지 않도록 한다.
     */
    private fun toResponses(
        posts: Page<Post>
    ): Page<PostResponse> {

        val imageUrls = profileService.getImageUrlsByMemberIds(
            posts.content.mapNotNull { post -> post.member.getId() }
        )

        return posts.map { post ->

            PostResponse.of(
                post,
                imageUrls[post.member.getId()]
            )
        }
    }

    /**
     * categoryId가 없으면 기존 카테고리를 그대로 둔다. (Profile의 직종 수정과 같은 규칙)
     */
    private fun resolveCategory(
        categoryId: Long?,
        current: JobCategory?
    ): JobCategory? {

        if (categoryId == null) {

            return current
        }

        return jobCategoryService.getJobCategory(categoryId)
    }

    private fun validatePostAuthor(
        post: Post
    ) {

        val member = getCurrentMember()

        if (post.member.getId() != member.getId()) {

            throw ApplicationException(PostStatusCode.FORBIDDEN_POST_ACCESS)
        }
    }

    private fun getCurrentMember() =
        SecurityContextHolder.getContext().authentication?.name
            ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
            ?.let { username -> memberRepository.findByUsername(username) }
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

    /**
     * 게시글에는 이미지가 최소 1개 있어야 한다.
     * 공백만 담긴 목록은 정규화 후 비므로 검증 애노테이션만으로는 걸러지지 않는다.
     */
    private fun requireAttachments(
        fileUrls: List<String>
    ): List<String> {

        if (fileUrls.isEmpty()) {

            throw ApplicationException(PostStatusCode.POST_IMAGE_REQUIRED)
        }

        return fileUrls
    }

    /**
     * 첨부는 업로드 경로(/files/<파일명>)만 받는다.
     * 외부 URL이 들어가면 프론트 next/image가 렌더링에 실패하고 열람자 추적에도 쓰일 수 있다.
     */
    private fun normalizeFileUrls(
        fileUrls: List<String>
    ): List<String> {

        val normalized = fileUrls.map { fileUrl -> fileUrl.trim() }
            .filter { fileUrl -> fileUrl.isNotBlank() }
            .distinct()

        if (normalized.any { fileUrl -> !UPLOADED_FILE_URL_REGEX.matches(fileUrl) }) {

            throw ApplicationException(PostStatusCode.INVALID_POST_FILE_URL)
        }

        return normalized
    }

    /**
     * 첨부를 fileUrls로 통째로 교체한다.
     * 새 목록에 남아 있는 파일은 그대로 쓰이므로, 빠진 파일만 커밋 후 삭제한다.
     */
    private fun replaceAttachments(
        post: Post,
        fileUrls: List<String>
    ) {

        val oldFileUrls = post.attachments
            .mapNotNull { attachment -> attachment.fileUrl }

        if (oldFileUrls == fileUrls) {

            return
        }

        post.attachments.clear()

        fileUrls.forEach { fileUrl -> post.addAttachment(fileUrl) }

        registerAttachedFileCommitCleanup(oldFileUrls - fileUrls.toSet())
    }

    private fun registerAttachedFileCommitCleanup(
        fileUrls: List<String>
    ) {

        if (fileUrls.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {

            override fun afterCommit() {

                fileUrls.forEach {

                    fileUrl -> fileService.deleteFile(fileUrl)
                }
            }
        })
    }

    companion object {

        private val POST_SORT_ALLOWED = setOf("updatedAt", "viewCount", "id", "repeatRate")

        /**
         * repeatRate 전용 @Query 는 CASE WHEN 으로 하드코딩된 order by 를 쓴다.
         * 응답 Page.sort 가 sorted:true 로 보이도록 PageRequest 에 같은 모양의 Sort 를 함께 실어 보낸다.
         */
        private val REPEAT_RATE_SORT_HINT = Sort.by(
            Sort.Order.desc("repeatRate"),
            Sort.Order.desc("viewCount"),
            Sort.Order.desc("id")
        )

        private val POST_SORT_DEFAULT = Sort.by(Sort.Direction.DESC, "updatedAt")

        private val POST_SORT_TIEBREAKER = Sort.by(Sort.Direction.DESC, "id")

        private const val VIEW_COUNT_KEY_PREFIX = "post-view:"

        private const val VIEW_COUNT_ANON_KEY_PREFIX = "post-view:anon:"

        private val VIEW_COUNT_TTL = Duration.ofDays(1)

        private val VIEW_COUNT_ANON_TTL = Duration.ofMinutes(5)

        private val UPLOADED_FILE_URL_REGEX = Regex("^/files/[^/\\s.][^/\\s]*$")
    }
}
