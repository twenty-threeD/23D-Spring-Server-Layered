package spring.springserver.domain.community.comment.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.domain.community.like.entity.CommunityCommentLike
import spring.springserver.domain.community.like.repository.CommunityCommentLikeRepository
import spring.springserver.domain.community.shared.service.CommunityAuthorizationService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.LocalDateTime

@Service
@Transactional
class CommunityCommentServiceImpl(private val communityCommentRepository: CommunityCommentRepository,
                                        private val communityCommentLikeRepository: CommunityCommentLikeRepository,
                                        private val communityAuthorizationService: CommunityAuthorizationService, ) : CommunityCommentService {

    override fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(createCommentRequest.postId)

        val communityComment = communityCommentRepository.save(
            CommunityComment(
                member = member,
                communityPost = communityPost,
                content = createCommentRequest.content.trim(),
                isEdited = false,
            )
        )

        return toCommentResponse(communityComment)
    }

    @Transactional(readOnly = true)
    override fun getComments(postId: Long): List<CommunityCommentResponse> {

        communityAuthorizationService.getActivePost(postId)

        return communityCommentRepository
            .findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
            .map(::toCommentResponse)
    }

    override fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(updateCommentRequest.commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.update(updateCommentRequest.content.trim())

        return toCommentResponse(communityComment)
    }

    override fun deleteComment(commentId: Long) {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.softDelete(LocalDateTime.now())
    }

    override fun likeComment(communityCommentLikeRequest: CommunityCommentLikeRequest): CommunityLikeResponse {

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

        return CommunityLikeResponse.of(
            targetId = commentId,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(commentId),
            message = "댓글 좋아요가 등록되었습니다.",
        )
    }

    override fun unlikeComment(commentId: Long): CommunityLikeResponse {

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

    private fun toCommentResponse(communityComment: CommunityComment): CommunityCommentResponse {

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(communityComment.getId()!!),
        )
    }
}
