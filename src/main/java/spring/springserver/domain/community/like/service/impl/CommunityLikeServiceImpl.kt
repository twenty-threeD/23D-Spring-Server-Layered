package spring.springserver.domain.community.like.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.like.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.domain.community.like.entity.CommunityCommentLike
import spring.springserver.domain.community.like.repository.CommunityCommentLikeRepository
import spring.springserver.domain.community.like.service.CommunityLikeService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityLikeServiceImpl(
    private val communityAuthorizationService: CommunityAuthorizationService,
    private  val communityCommentLikeRepository: CommunityCommentLikeRepository
): CommunityLikeService {

    override fun likeComment(
        communityCommentLikeRequest: CommunityCommentLikeRequest
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val commentId = communityCommentLikeRequest.commentId

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        if (communityCommentLikeRepository.existsByMemberAndCommunityComment(member, communityComment)) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "이미 좋아요를 누른 댓글입니다."
            )
        }

        communityCommentLikeRepository.save(
            CommunityCommentLike(
                member = member,
                communityComment = communityComment,
            )
        )

        return CommunityLikeResponse.Companion.of(
            targetId = commentId,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(commentId),
            message = "댓글 좋아요가 등록되었습니다.",
        )
    }

    override fun unlikeComment(
        commentId: Long
    ): CommunityLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        val deletedCount = communityCommentLikeRepository.deleteByMemberAndCommunityComment(
            member,
            communityComment
        )

        if (deletedCount == 0L) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "좋아요를 누르지 않은 댓글입니다."
            )
        }

        return CommunityLikeResponse.of(
            targetId = commentId,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(commentId),
            message = "댓글 좋아요가 취소되었습니다.",
        )
    }
}