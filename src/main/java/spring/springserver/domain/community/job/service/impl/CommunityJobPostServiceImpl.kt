package spring.springserver.domain.community.job.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.exception.CommunityStatusCode
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.data.response.CommunityJobPostResponse
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
    private val applicationEventPublisher: ApplicationEventPublisher
): CommunityJobPostService {

    override fun createJobPost(
        createJobPostRequest: CreateJobPostRequest
    ): CommunityJobPostResponse {

        val member = communityAuthorizationService.getCurrentMember()

        validatePhoneVerified(member)

        val postType = createJobPostRequest.postType!!
        val jobCategory = jobCategoryService.getJobCategory(createJobPostRequest.jobCategoryId!!)
        val sig = locationService.getSig(createJobPostRequest.sigCd!!.trim())

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

        communityJobPost.update(
            postType = updateJobPostRequest.postType!!,
            title = updateJobPostRequest.title!!.trim(),
            content = updateJobPostRequest.content?.trim()?.takeIf { it.isNotBlank() },
            fileUrl = updateJobPostRequest.fileUrl?.trim()?.takeIf { it.isNotBlank() },
            jobCategory = jobCategoryService.getJobCategory(updateJobPostRequest.jobCategoryId!!),
            sig = locationService.getSig(updateJobPostRequest.sigCd!!.trim())
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
    ): List<CommunityJobPostResponse> {

        val postTypes = searchJobPostRequest.postType?.let { listOf(it) }
            ?: JobPostType.entries

        val jobCategoryId = searchJobPostRequest.jobCategoryId

        val jobCategoryIds = jobCategoryId
            ?.let { jobCategoryService.getCategoryIdsIncludingDescendants(it) }
            ?.ifEmpty { listOf(NO_FILTER_ID) }
            ?: listOf(NO_FILTER_ID)

        val sigCds = resolveSigCds(
            searchJobPostRequest.sigCd,
            searchJobPostRequest.nearbyOnly
        )

        return communityJobPostRepository.searchJobPosts(
            postTypes = postTypes,
            jobCategoryId = jobCategoryId,
            jobCategoryIds = jobCategoryIds,
            sigCdFilter = sigCds?.firstOrNull(),
            sigCds = sigCds ?: listOf(NO_FILTER_SIG_CD),
            keyword = searchJobPostRequest.keyword?.trim().orEmpty()
        ).map { communityJobPost -> toResponse(communityJobPost) }
    }

    /**
     * 조회수를 올려야 하므로 readOnly로 두지 않는다.
     * readOnly면 flush가 일어나지 않아 증가분이 저장되지 않는다.
     */
    override fun getJobPost(
        postId: Long
    ): CommunityJobPostResponse {

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        communityJobPost.increaseViewCount()

        return toResponse(communityJobPost)
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

    private fun toResponse(
        communityJobPost: CommunityJobPost
    ): CommunityJobPostResponse {

        return CommunityJobPostResponse.toJobPostResponse(
            communityJobPost,
            communityJobCommentRepository,
            communityJobPostLikeRepository
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
