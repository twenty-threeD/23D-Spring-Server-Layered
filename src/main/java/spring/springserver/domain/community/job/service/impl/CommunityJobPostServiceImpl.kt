package spring.springserver.domain.community.job.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.exception.CommunityStatusCode
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.data.response.CommunityJobPostResponse
import spring.springserver.domain.community.job.data.response.JobPostPageResponse
import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.entity.JobPostType
import spring.springserver.domain.community.job.event.JobPostCreatedEvent
import spring.springserver.domain.community.job.repository.CommunityJobCommentRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostLikeRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostRepository
import spring.springserver.domain.community.job.service.CommunityJobAuthorizationService
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.domain.location.service.LocationService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.profile.repository.ProfileRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityJobPostServiceImpl(
    private val communityJobPostRepository: CommunityJobPostRepository,
    private val communityJobCommentRepository: CommunityJobCommentRepository,
    private val communityJobPostLikeRepository: CommunityJobPostLikeRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val communityJobAuthorizationService: CommunityJobAuthorizationService,
    private val jobCategoryService: JobCategoryService,
    private val locationService: LocationService,
    private val profileRepository: ProfileRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val redisTemplate: RedisTemplate<String, String>
): CommunityJobPostService {

    override fun createJobPost(
        createJobPostRequest: CreateJobPostRequest
    ): CommunityJobPostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        validatePhoneVerified(member)

        /**
         * 구인/구직 구분과 지역은 프론트가 보내지 않을 수 있어 기본값으로 채운다.
         * 지역은 알림 대상 선정의 기준점이라 비워 둘 수 없으므로,
         * 요청에도 프로필에도 없으면 그때는 막는다.
         */
        val postType = createJobPostRequest.postType
            ?: JobPostType.HIRING

        val jobCategory = jobCategoryService.getJobCategory(createJobPostRequest.jobCategoryId!!)

        val sigCd = createJobPostRequest.sigCd?.trim()?.takeIf { it.isNotBlank() }
            ?: currentMemberSigCdOrNull()
            ?: throw ApplicationException(CommunityStatusCode.REGION_NOT_SET)

        val sig = locationService.getSig(sigCd)

        val communityJobPost = communityJobPostRepository.save(
            CommunityJobPost(
                member = member,
                username = member.username,
                postType = postType,
                title = createJobPostRequest.title!!.trim(),
                content = createJobPostRequest.content?.trim()?.takeIf { it.isNotBlank() },
                fileUrl = createJobPostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
                jobCategory = jobCategory,
                sig = sig
            )
        )

        applicationEventPublisher.publishEvent(
            JobPostCreatedEvent(
                postId = communityJobPost.getId()!!,
                writerMemberId = member.getId()!!,
                postType = postType,
                title = communityJobPost.title,
                jobCategoryId = jobCategory.getId()!!,
                sigCd = sig.getSigCd()
            )
        )

        return toResponse(communityJobPost)
    }

    override fun updateJobPost(
        updateJobPostRequest: UpdateJobPostRequest
    ): CommunityJobPostResponse {

        val communityJobPost = getOwnedJobPost(updateJobPostRequest.postId!!)

        /**
         * 넘기지 않은 구분·지역은 기존 값을 그대로 둔다.
         */
        val sig = updateJobPostRequest.sigCd?.trim()?.takeIf { it.isNotBlank() }
            ?.let { sigCd -> locationService.getSig(sigCd) }
            ?: communityJobPost.sig

        communityJobPost.update(
            postType = updateJobPostRequest.postType ?: communityJobPost.postType,
            title = updateJobPostRequest.title!!.trim(),
            content = updateJobPostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updateJobPostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
            jobCategory = jobCategoryService.getJobCategory(updateJobPostRequest.jobCategoryId!!),
            sig = sig
        )

        return toResponse(communityJobPost)
    }

    override fun deleteJobPost(
        postId: Long
    ): DeleteResponse {

        getOwnedJobPost(postId).softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }

    @Transactional(readOnly = true)
    override fun getJobPosts(
        searchJobPostRequest: SearchJobPostRequest
    ): JobPostPageResponse {

        val postTypes = searchJobPostRequest.postType?.let { listOf(it) }
            ?: JobPostType.entries

        val jobCategoryId = searchJobPostRequest.resolvedJobCategoryId()

        val jobCategoryIds = jobCategoryId
            ?.let { jobCategoryService.getCategoryIdsIncludingDescendants(it) }
            ?.ifEmpty { listOf(NO_FILTER_ID) }
            ?: listOf(NO_FILTER_ID)

        val sigCds = resolveSigCds(
            searchJobPostRequest.sigCd,
            searchJobPostRequest.nearbyOnly
        )

        /**
         * 대상 시군구가 비어 있으면 "주변에 아무것도 없음"이므로 매칭되지 않는 더미 값을 넣는다.
         * 여기서 필터를 꺼 버리면 조건이 사라져 전국 글이 전부 나온다.
         */
        val postIds = communityJobPostRepository.searchJobPostIds(
            postTypes = postTypes,
            applyCategoryFilter = jobCategoryId != null,
            jobCategoryIds = jobCategoryIds,
            applySigFilter = sigCds != null,
            sigCds = sigCds?.ifEmpty { listOf(NO_FILTER_SIG_CD) } ?: listOf(NO_FILTER_SIG_CD),
            keyword = searchJobPostRequest.keyword?.trim().orEmpty(),
            pageable = PageRequest.of(searchJobPostRequest.page, searchJobPostRequest.size)
        )

        return JobPostPageResponse.of(
            page = postIds,
            content = toResponses(findOrderedByIds(postIds.content)),
            nearbyFilterApplied = searchJobPostRequest.nearbyOnly.takeIf { it }?.let { sigCds != null }
        )
    }

    /**
     * in 절 조회는 순서를 보장하지 않으므로 페이징 쿼리가 준 id 순서로 다시 세운다.
     */
    private fun findOrderedByIds(
        postIds: List<Long>
    ): List<CommunityJobPost> {

        if (postIds.isEmpty()) {

            return emptyList()
        }

        val communityJobPosts = communityJobPostRepository.findAllWithAssociationsByIds(postIds)
            .associateBy { communityJobPost -> communityJobPost.getId() }

        return postIds.mapNotNull { postId -> communityJobPosts[postId] }
    }

    /**
     * 목록은 댓글 수·좋아요 수·좋아요 여부를 글 수와 무관하게 세 번의 쿼리로 모아 온다.
     * 글마다 집계 쿼리를 날리면 글이 쌓일수록 그대로 느려진다.
     */
    private fun toResponses(
        communityJobPosts: List<CommunityJobPost>
    ): List<CommunityJobPostResponse> {

        if (communityJobPosts.isEmpty()) {

            return emptyList()
        }

        val postIds = communityJobPosts.mapNotNull { communityJobPost -> communityJobPost.getId() }

        val commentCounts = communityJobCommentRepository.countCommentsByPostIds(postIds)
            .associate { row -> row.getPostId() to row.getCount() }

        val likeCounts = communityJobPostRepository.countLikesByPostIds(postIds)
            .associate { row -> row.getPostId() to row.getCount() }

        val likedPostIds = currentMemberIdOrNull()
            ?.let { memberId -> communityJobPostRepository.findLikedPostIds(postIds, memberId) }
            ?.toSet()
            ?: emptySet()

        return communityJobPosts.map {

            communityJobPost ->
            val postId = communityJobPost.getId()

            CommunityJobPostResponse.of(
                communityJobPost = communityJobPost,
                commentCount = commentCounts[postId] ?: 0L,
                likeCount = likeCounts[postId] ?: 0L,
                isLiked = postId in likedPostIds
            )
        }
    }

    /**
     * 비로그인 조회에서도 목록은 내려가야 하므로 회원을 못 찾으면 null로 둔다.
     */
    private fun currentMemberIdOrNull(): Long? {

        return communityAuthorizationService.getCurrentMemberOrNull()?.getId()
    }

    /**
     * 조회수를 올려야 하므로 readOnly로 두지 않는다.
     * readOnly면 flush가 일어나지 않아 증가분이 저장되지 않는다.
     */
    override fun getJobPost(
        postId: Long
    ): CommunityJobPostResponse {

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        if (shouldCountView(communityJobPost)) {

            communityJobPost.increaseViewCount()
        }

        return toResponse(communityJobPost)
    }

    /**
     * 작성자 본인의 조회와 같은 회원의 재조회는 세지 않는다.
     * Redis 키에 TTL을 걸어 회원 한 명당 글 하나를 하루에 한 번만 반영한다.
     *
     * Redis가 죽어 있으면 조회수를 세지 않는 쪽으로 넘어간다.
     * 상세 조회 자체가 실패하는 것보다 집계가 조금 비는 편이 낫다.
     */
    private fun shouldCountView(
        communityJobPost: CommunityJobPost
    ): Boolean {

        val memberId = currentMemberIdOrNull()
            ?: return false

        if (memberId == communityJobPost.member.getId()) {

            return false
        }

        val key = "$VIEW_COUNT_KEY_PREFIX${communityJobPost.getId()}:$memberId"

        return runCatching {

            redisTemplate.opsForValue()
                .setIfAbsent(key, "1", VIEW_COUNT_TTL)
                ?: false
        }.getOrDefault(false)
    }

    /**
     * 지역 필터로 쓸 시군구 코드 목록. 필터를 걸지 않을 때는 null이다.
     * nearbyOnly면 기준 시군구에서 반경 안에 드는 시군구까지 넓힌다.
     */
    private fun resolveSigCds(
        sigCd: String?,
        nearbyOnly: Boolean
    ): List<String>? {

        if (!nearbyOnly) {

            return sigCd?.trim()?.takeIf { it.isNotBlank() }?.let { listOf(it) }
        }

        /**
         * 기준 지역을 정할 수 없으면 지역 필터를 걸지 않는다(null).
         * 목록 조회 자체를 400으로 막으면 지역 미설정 회원은 글을 아예 볼 수 없다.
         */
        val baseSigCd = sigCd?.trim()?.takeIf { it.isNotBlank() }
            ?: currentMemberSigCdOrNull()
            ?: return null

        return locationService.findNearbySigCds(
            baseSigCd,
            CommunityJobPostService.NEARBY_RADIUS_KM
        )
    }

    /**
     * 구인/구직 글은 작성자 본인만 손댈 수 있다.
     */
    private fun getOwnedJobPost(
        postId: Long
    ): CommunityJobPost {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        communityAuthorizationService.validateOwner(member, communityJobPost.member.getId())

        return communityJobPost
    }

    /**
     * 비로그인이거나 프로필에 지역이 없으면 null이다.
     */
    private fun currentMemberSigCdOrNull(): String? {

        val member = communityAuthorizationService.getCurrentMemberOrNull()
            ?: return null

        return profileRepository.findByMember(member)?.sig?.getSigCd()
    }

    private fun validatePhoneVerified(
        member: Member
    ) {

        if (!member.isPhoneVerified()) {

            throw ApplicationException(MemberStatusCode.PHONE_NOT_VERIFIED)
        }
    }

    private fun toResponse(
        communityJobPost: CommunityJobPost
    ): CommunityJobPostResponse {

        val memberId = currentMemberIdOrNull()

        val isLiked = memberId != null
            && communityJobPostRepository.findLikedPostIds(
                listOf(communityJobPost.getId()!!),
                memberId
            ).isNotEmpty()

        return CommunityJobPostResponse.toJobPostResponse(
            communityJobPost,
            communityJobCommentRepository,
            communityJobPostLikeRepository,
            isLiked
        )
    }

    companion object {

        /**
         * in 절에 빈 컬렉션이 들어가지 않도록 채워 넣는 값이다.
         * 실제로 존재할 수 없는 값이라 어떤 행과도 매칭되지 않는다.
         */
        private const val NO_FILTER_ID = -1L

        private const val NO_FILTER_SIG_CD = "-"

        private const val VIEW_COUNT_KEY_PREFIX = "job-post-view:"

        private val VIEW_COUNT_TTL = Duration.ofDays(1)
    }
}
