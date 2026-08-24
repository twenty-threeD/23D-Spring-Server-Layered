package spring.springserver.domain.post.service.impl

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.service.PostService
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class PostServiceImpl(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val fileService: FileService,
    private val jobCategoryService: JobCategoryService,
    private val profileService: ProfileService
): PostService {

    override fun createPost(
        createPostRequest: CreatePostRequest
    ): PostResponse {

        val member = getCurrentMember()

        val post = createPostRequest.toEntity(
            member,
            resolveCategory(createPostRequest.categoryId, null)
        )

        requireAttachments(normalizeFileUrls(createPostRequest.fileUrl.orEmpty()))
            .forEach { fileUrl -> post.addAttachment(fileUrl) }

        return toResponse(postRepository.save(post))
    }

    override fun viewPost(
        id: Long
    ): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        val updatePost = postRepository.incrementViewCount(id)

        if (updatePost == 0) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        val updatedPost = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        return toResponse(updatedPost)
    }

    override fun viewAllPosts(
        pageable: Pageable
    ): Page<PostResponse> {

        return toResponses(
            postRepository.findAllByIsDeletedFalseOrderByUpdatedAtDesc(pageable)
        )
    }

    override fun searchPosts(
        title: String?,
        categoryId: Long?,
        pageable: Pageable
    ): Page<PostResponse> {

        val normalizedTitle = title?.trim().orEmpty()

        if (categoryId == null) {

            return toResponses(
                postRepository.searchPostsByTitle(
                    normalizedTitle,
                    pageable.withoutSort()
                )
            )
        }

        return toResponses(
            postRepository.searchPostsByTitleAndCategoryIds(
                normalizedTitle,
                jobCategoryService.getCategoryIdsIncludingDescendants(categoryId),
                pageable.withoutSort()
            )
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
            memberId?.let { id -> profileService.getImageUrlsByMemberIds(listOf(id))[id] }
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

    private fun normalizeFileUrls(
        fileUrls: List<String>
    ): List<String> {

        return fileUrls.map { fileUrl -> fileUrl.trim() }
            .filter { fileUrl -> fileUrl.isNotBlank() }
            .distinct()
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

    private fun Pageable.withoutSort(): Pageable =
        PageRequest.of(pageNumber, pageSize)
}
