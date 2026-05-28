package spring.springserver.domain.community.comment.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityCommentLikeRepository
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityCommentServiceImpl(private val communityCommentRepository: CommunityCommentRepository,
                                        private val communityCommentLikeRepository: CommunityCommentLikeRepository,
                                        private val communityAuthorizationService: CommunityAuthorizationService) : CommunityCommentService {

    override fun createComment(createCommentRequest: CreateCommentRequest): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(createCommentRequest.postId)

        val communityComment = communityCommentRepository.save(CommunityComment(
                member = member,
                communityPost = communityPost,
                content = createCommentRequest.content.trim(),
                isEdited = false,
            )
        )

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(communityComment.getId()!!),
        )
    }

    @Transactional(readOnly = true)
    override fun getComments(postId: Long): List<CommunityCommentResponse> {

        communityAuthorizationService.getActivePost(postId)

        return communityCommentRepository
            .findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
            .map {

                communityComment -> CommunityCommentResponse.of(
                    communityComment = communityComment,
                    likeCount = communityCommentLikeRepository.countByCommunityCommentId(communityComment.getId()!!),
                )
            }
    }

    override fun updateComment(updateCommentRequest: UpdateCommentRequest): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(updateCommentRequest.commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.update(updateCommentRequest.content.trim())

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = communityCommentLikeRepository.countByCommunityCommentId(communityComment.getId()!!),
        )
    }

    override fun deleteComment(commentId: Long): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.softDelete(LocalDateTime.now())

        return DeleteResponse.of("삭제되었습니다.")
    }
}
