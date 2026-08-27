package spring.springserver.domain.community.job.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.exception.CommunityStatusCode
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.event.JobPostCreatedEvent
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.entity.CommunityPostType
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.domain.location.service.LocationService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.profile.repository.ProfileRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityJobPostServiceImpl(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityPostLikeRepository: CommunityPostLikeRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val jobCategoryService: JobCategoryService,
    private val locationService: LocationService,
    private val profileRepository: ProfileRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
): CommunityJobPostService {

    override fun createJobPost(
        createJobPostRequest: CreateJobPostRequest
    ): CommunityPostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        validatePhoneVerified(member)

        val postType = validateJobPostType(createJobPostRequest.postType!!)
        val jobCategory = jobCategoryService.getJobCategory(createJobPostRequest.jobCategoryId!!)
        val sig = locationService.getSig(createJobPostRequest.sigCd!!.trim())

        val communityPost = communityPostRepository.save(
            CommunityPost(
                member = member,
                username = member.username,
                title = createJobPostRequest.title!!.trim(),
                content = createJobPostRequest.content?.trim()?.takeIf { it.isNotBlank() },
                fileUrl = createJobPostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
                postType = postType,
                jobCategory = jobCategory,
                sig = sig
            )
        )

        applicationEventPublisher.publishEvent(
            JobPostCreatedEvent(
                postId = communityPost.getId()!!,
                writerMemberId = member.getId()!!,
                postType = postType,
                title = communityPost.title,
                jobCategoryId = jobCategory.getId()!!,
                sigCd = sig.getSigCd()
            )
        )

        return toResponse(communityPost)
    }

    override fun updateJobPost(
        updateJobPostRequest: UpdateJobPostRequest
    ): CommunityPostResponse {

        val communityPost = getOwnedJobPost(updateJobPostRequest.postId!!)

        communityPost.updateJobPost(
            title = updateJobPostRequest.title!!.trim(),
            content = updateJobPostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updateJobPostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
            postType = validateJobPostType(updateJobPostRequest.postType!!),
            jobCategory = jobCategoryService.getJobCategory(updateJobPostRequest.jobCategoryId!!),
            sig = locationService.getSig(updateJobPostRequest.sigCd!!.trim())
        )

        return toResponse(communityPost)
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
    ): List<CommunityPostResponse> {

        val postTypes = searchJobPostRequest.postType?.let { listOf(validateJobPostType(it)) }
            ?: CommunityPostType.jobPostTypes()

        val jobCategoryId = searchJobPostRequest.jobCategoryId

        val jobCategoryIds = jobCategoryId
            ?.let { jobCategoryService.getCategoryIdsIncludingDescendants(it) }
            ?.ifEmpty { listOf(NO_FILTER_ID) }
            ?: listOf(NO_FILTER_ID)

        val sigCds = resolveSigCds(
            searchJobPostRequest.sigCd,
            searchJobPostRequest.nearbyOnly
        )

        return communityPostRepository.searchJobPosts(
            postTypes = postTypes,
            jobCategoryId = jobCategoryId,
            jobCategoryIds = jobCategoryIds,
            sigCdFilter = sigCds?.firstOrNull(),
            sigCds = sigCds ?: listOf(NO_FILTER_SIG_CD),
            keyword = searchJobPostRequest.keyword?.trim().orEmpty()
        ).map { communityPost -> toResponse(communityPost) }
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

        val baseSigCd = sigCd?.trim()?.takeIf { it.isNotBlank() }
            ?: currentMemberSigCd()

        return locationService.findNearbySigCds(
            baseSigCd,
            CommunityJobPostService.NEARBY_RADIUS_KM
        )
    }

    /**
     * 구인/구직 API로는 구인/구직 글만, 그것도 작성자 본인만 손댈 수 있다.
     */
    private fun getOwnedJobPost(
        postId: Long
    ): CommunityPost {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(postId)

        communityAuthorizationService.validateOwner(member, communityPost.member.getId())

        if (!communityPost.isJobPost()) {

            throw ApplicationException(CommunityStatusCode.NOT_JOB_POST)
        }

        return communityPost
    }

    private fun currentMemberSigCd(): String {

        val member = communityAuthorizationService.getCurrentMember()

        return profileRepository.findByMember(member)?.sig?.getSigCd()
            ?: throw ApplicationException(CommunityStatusCode.REGION_NOT_SET)
    }

    private fun validatePhoneVerified(
        member: Member
    ) {

        if (!member.isPhoneVerified()) {

            throw ApplicationException(MemberStatusCode.PHONE_NOT_VERIFIED)
        }
    }

    private fun validateJobPostType(
        postType: CommunityPostType
    ): CommunityPostType {

        if (!postType.isJobPost()) {

            throw ApplicationException(CommunityStatusCode.NOT_JOB_POST)
        }

        return postType
    }

    private fun toResponse(
        communityPost: CommunityPost
    ): CommunityPostResponse {

        return CommunityPostResponse.toPostResponse(
            communityPost,
            communityCommentRepository,
            communityPostLikeRepository
        )
    }

    companion object {

        /**
         * in 절에 빈 컬렉션이 들어가지 않도록 채워 넣는 값이다.
         * 실제로 존재할 수 없는 값이라 어떤 행과도 매칭되지 않는다.
         */
        private const val NO_FILTER_ID = -1L

        private const val NO_FILTER_SIG_CD = "-"
    }
}
