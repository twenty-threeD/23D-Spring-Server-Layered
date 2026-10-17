package spring.springserver.domain.community.comment.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
import spring.springserver.global.data.PageResponse
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
        postId: Long,
        pageable: Pageable
    ): PageResponse<CommunityCommentResponse> {

        communityAuthorizationService.getActivePost(postId)

        /**
         * 정렬은 작성순으로 고정하고 페이지 번호·크기만 받는다.
         */
        val rootCommentPage = communityCommentRepository.findRootCommentsByPostId(
            postId,
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize
            )
        )

        val rootComments = rootCommentPage.content

        val rootCommentIds = rootComments.mapNotNull { comment -> comment.getId() }

        val replies = if (rootCommentIds.isEmpty()) {

            emptyList()
        } else {

            communityCommentRepository.findRepliesByParentIds(rootCommentIds)
        }

        val imageUrls = profileService.getImageUrlsByMemberIds(
            (rootComments + replies).mapNotNull { comment -> comment.member.getId() }
        )

        val repliesByParentId = replies.groupBy { reply -> reply.parent?.getId() }

        val content = rootComments.map { rootComment ->

            val replyResponses = repliesByParentId[rootComment.getId()]
                .orEmpty()
                .map { reply ->

                    CommunityCommentResponse.of(
                        communityComment = reply,
                        likeCount = 0L,
                        imageUrl = imageUrls[reply.member.getId()]
                    )
                }

            CommunityCommentResponse.of(
                communityComment = rootComment,
                likeCount = 0L,
                imageUrl = imageUrls[rootComment.member.getId()],
                replies = replyResponses
            )
        }

        return PageResponse.of(
            rootCommentPage,
            content
        )
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
