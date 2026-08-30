package spring.springserver.domain.community.job.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.JobPostLikeRequest
import spring.springserver.domain.community.job.entity.CommunityJobPostLike
import spring.springserver.domain.community.job.repository.CommunityJobPostLikeRepository
import spring.springserver.domain.community.job.service.CommunityJobAuthorizationService
import spring.springserver.domain.community.job.service.CommunityJobLikeService
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityJobLikeServiceImpl(
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val communityJobAuthorizationService: CommunityJobAuthorizationService,
    private val communityJobPostLikeRepository: CommunityJobPostLikeRepository
): CommunityJobLikeService {

    override fun likeJobPost(
        jobPostLikeRequest: JobPostLikeRequest
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val postId = jobPostLikeRequest.postId!!

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        if (communityJobPostLikeRepository.existsByMemberAndCommunityJobPost(member, communityJobPost)) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "이미 좋아요를 누른 게시글입니다."
            )
        }

        communityJobPostLikeRepository.save(
            CommunityJobPostLike(
                member = member,
                communityJobPost = communityJobPost,
            )
        )

        return CommunityLikeResponse.of(
            targetId = postId,
            likeCount = communityJobPostLikeRepository.countByCommunityJobPostId(postId),
            message = "게시글 좋아요가 등록되었습니다.",
        )
    }

    override fun unlikeJobPost(
        postId: Long
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        val deletedCount = communityJobPostLikeRepository.deleteByMemberAndCommunityJobPost(
            member,
            communityJobPost
        )

        if (deletedCount == 0L) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "좋아요를 누르지 않은 게시글입니다."
            )
        }

        return CommunityLikeResponse.of(
            targetId = postId,
            likeCount = communityJobPostLikeRepository.countByCommunityJobPostId(postId),
            message = "게시글 좋아요가 취소되었습니다.",
        )
    }
}
