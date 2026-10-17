package spring.springserver.domain.community.comment.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.comment.service.CommunityCommentService
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.common.exception.CommunityStatusCode
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityCommentServiceImpl(
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val profileService: ProfileService
): CommunityCommentService {

    override fun createComment(
        createCommentRequest: CreateCommentRequest
    ): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityPost = communityAuthorizationService.getActivePost(createCommentRequest.postId)

        val parent = createCommentRequest.parentId?.let { parentId ->

            val found = communityCommentRepository.findByIdAndDeletedAtIsNull(parentId)
                ?: throw ApplicationException(CommunityStatusCode.COMMENT_PARENT_NOT_FOUND)

            if (found.communityPost.getId() != communityPost.getId()) {

                throw ApplicationException(CommunityStatusCode.COMMENT_PARENT_POST_MISMATCH)
            }

            if (found.isReply()) {

                throw ApplicationException(CommunityStatusCode.COMMENT_REPLY_DEPTH_EXCEEDED)
            }

            found
        }

        val communityComment = communityCommentRepository.save(
            CommunityComment(
                member = member,
                communityPost = communityPost,
                content = createCommentRequest.content.trim(),
                isEdited = false,
                parent = parent
            )
        )

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = 0L,
            imageUrl = getImageUrl(communityComment)
        )
    }

    @Transactional(readOnly = true)
    override fun getComments(
        postId : Long
    ): List<CommunityCommentResponse> {

        communityAuthorizationService.getActivePost(postId)

        /**
         * 삭제된 원댓글도 답글이 남아 있으면 placeholder 로 보여야 하므로 모두 가져온다.
         * parent·member 는 fetch join 으로 묶어 두 번째 쿼리가 돌지 않게 한다.
         */
        val allComments = communityCommentRepository.findAllForPostWithParent(postId)

        val imageUrls = profileService.getImageUrlsByMemberIds(
            allComments.mapNotNull { comment -> comment.member.getId() }
        )

        val repliesByParentId = allComments
            .filter { comment -> comment.isReply() && !comment.isDeleted() }
            .groupBy { comment -> comment.parent?.getId() }

        return allComments
            .asSequence()
            .filter { comment -> !comment.isReply() }
            .filter { parent ->

                /**
                 * 삭제된 원댓글은 답글이 남아 있을 때만 placeholder 로 노출한다.
                 */
                !parent.isDeleted() || !repliesByParentId[parent.getId()].isNullOrEmpty()
            }
            .map { parent ->

                val replies = repliesByParentId[parent.getId()]
                    .orEmpty()
                    .map { reply ->

                        CommunityCommentResponse.of(
                            communityComment = reply,
                            likeCount = 0L,
                            imageUrl = imageUrls[reply.member.getId()]
                        )
                    }

                CommunityCommentResponse.of(
                    communityComment = parent,
                    likeCount = 0L,
                    imageUrl = imageUrls[parent.member.getId()],
                    replies = replies
                )
            }
            .toList()
    }

    override fun updateComment(
        updateCommentRequest: UpdateCommentRequest
    ): CommunityCommentResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(updateCommentRequest.commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        communityComment.update(updateCommentRequest.content.trim())

        return CommunityCommentResponse.of(
            communityComment = communityComment,
            likeCount = 0L,
            imageUrl = getImageUrl(communityComment)
        )
    }

    override fun deleteComment(
        commentId: Long
    ): DeleteResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityComment = communityAuthorizationService.getActiveComment(commentId)

        communityAuthorizationService.validateOwner(
            member,
            communityComment.member.getId()
        )

        /**
         * 답글이 매달린 원댓글은 자리가 사라지면 답글 흐름이 깨진다. soft delete 로 placeholder 를 남긴다.
         * 답글이 없거나 자신이 답글이면 그냥 지운다.
         */
        val commentId = communityComment.getId()
        val hasReplies = commentId != null && communityCommentRepository.existsByParentId(commentId)

        if (hasReplies) {

            communityComment.softDelete(LocalDateTime.now())
        } else {

            communityCommentRepository.delete(communityComment)
        }

        return DeleteResponse.of("삭제되었습니다.")
    }

    private fun getImageUrl(
        communityComment: CommunityComment
    ): String? {

        return communityComment.member.getId()
            ?.let { memberId -> profileService.getImageUrlByMemberId(memberId) }
    }
}
