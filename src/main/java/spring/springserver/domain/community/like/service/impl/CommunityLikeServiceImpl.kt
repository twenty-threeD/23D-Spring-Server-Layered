package spring.springserver.domain.community.like.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.like.data.request.CommunityPostLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.domain.community.like.entity.CommunityPostLike
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.like.service.CommunityLikeService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityLikeServiceImpl(
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val communityPostLikeRepository: CommunityPostLikeRepository
): CommunityLikeService {

    override fun likePost(
        communityPostLikeRequest: CommunityPostLikeRequest
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val postId = communityPostLikeRequest.postId

        val communityPost = communityAuthorizationService.getActivePost(postId)

        if (communityPostLikeRepository.existsByMemberAndCommunityPost(member, communityPost)) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "이미 좋아요를 누른 게시글입니다."
            )
        }

        communityPostLikeRepository.save(
            CommunityPostLike(
                member = member,
                communityPost = communityPost,
            )
        )

        return CommunityLikeResponse.of(
            targetId = postId,
            likeCount = communityPostLikeRepository.countByCommunityPostId(postId),
            message = "게시글 좋아요가 등록되었습니다.",
        )
    }

    override fun unlikePost(
        postId: Long
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(postId)

        val deletedCount = communityPostLikeRepository.deleteByMemberAndCommunityPost(
            member,
            communityPost
        )

        if (deletedCount == 0L) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "좋아요를 누르지 않은 게시글입니다."
            )
        }

        return CommunityLikeResponse.of(
            targetId = postId,
            likeCount = communityPostLikeRepository.countByCommunityPostId(postId),
            message = "게시글 좋아요가 취소되었습니다.",
        )
    }
}