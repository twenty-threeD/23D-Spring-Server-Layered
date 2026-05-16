package spring.springserver.domain.community.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.data.request.CreateCommentRequest
import spring.springserver.domain.community.data.request.UpdateCommentRequest
import spring.springserver.domain.community.data.response.CommunityCommentResponse
import spring.springserver.domain.community.data.response.CommunityLikeResponse
import spring.springserver.domain.community.entity.CommunityComment
import spring.springserver.domain.community.entity.CommunityCommentLike
import spring.springserver.domain.community.repository.CommunityCommentLikeRepository
import spring.springserver.domain.community.repository.CommunityCommentRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.LocalDateTime

@Service
@Transactional
class CommunityCommentServiceImpl(
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityCommentLikeRepository: CommunityCommentLikeRepository,
    private val communityAccessSupport: CommunityAccessSupport,
) : CommunityCommentService {

    override fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse {
        val member = communityAccessSupport.getCurrentMember()
        val communityPost = communityAccessSupport.getActivePost(createCommentRequest.postId)
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
        communityAccessSupport.getActivePost(postId)
        return communityCommentRepository.findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
            .map(::toCommentResponse)
    }

    override fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse {
        val member = communityAccessSupport.getCurrentMember()
        val communityComment = communityAccessSupport.getActiveComment(updateCommentRequest.commentId)
        communityAccessSupport.validateOwner(member, communityComment.member.getId())
        communityComment.update(updateCommentRequest.content.trim())
        return toCommentResponse(communityComment)
    }

    override fun deleteComment(commentId: Long) {
        val member = communityAccessSupport.getCurrentMember()
        val communityComment = communityAccessSupport.getActiveComment(commentId)
        communityAccessSupport.validateOwner(member, communityComment.member.getId())
        communityComment.softDelete(LocalDateTime.now())
    }

    override fun likeComment(communityCommentLikeRequest: CommunityCommentLikeRequest): CommunityLikeResponse {
        val member = communityAccessSupport.getCurrentMember()
        val commentId = communityCommentLikeRequest.commentId
        val communityComment = communityAccessSupport.getActiveComment(commentId)

        if (communityCommentLikeRepository.existsByMemberAndCommunityComment(member, communityComment)) {
            throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT, "이미 좋아요를 누른 댓글입니다.")
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
        val member = communityAccessSupport.getCurrentMember()
        val communityComment = communityAccessSupport.getActiveComment(commentId)
        val deletedCount = communityCommentLikeRepository.deleteByMemberAndCommunityComment(member, communityComment)

        if (deletedCount == 0L) {
            throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT, "좋아요를 누르지 않은 댓글입니다.")
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
