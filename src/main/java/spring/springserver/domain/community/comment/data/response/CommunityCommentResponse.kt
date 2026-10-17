package spring.springserver.domain.community.comment.data.response

import spring.springserver.domain.community.comment.entity.CommunityComment
import java.time.LocalDateTime

data class CommunityCommentResponse(
    val id: Long?,

    val postId: Long?,

    val parentId: Long?,

    val replyToCommentId: Long?,

    val replyToUsername: String?,

    val username: String,

    val imageUrl: String?,

    val content: String,

    val isEdited: Boolean,

    val isDeleted: Boolean,

    val likeCount: Long,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?,

    val replies: List<CommunityCommentResponse> = emptyList()
) {

    companion object {

        private const val DELETED_CONTENT_PLACEHOLDER = "삭제된 댓글입니다"

        fun of(
            communityComment: CommunityComment,
            likeCount: Long,
            imageUrl: String?,
            replies: List<CommunityCommentResponse> = emptyList()
        ): CommunityCommentResponse {

            val isDeleted = communityComment.isDeleted()
            val visibleContent = if (isDeleted) {

                DELETED_CONTENT_PLACEHOLDER
            } else {

                communityComment.content
            }

            /**
             * 삭제된 댓글은 작성자 식별 정보를 지워 자리만 남긴다.
             */
            val displayUsername = if (isDeleted) {

                DELETED_CONTENT_PLACEHOLDER
            } else {

                communityComment.member.getDisplayUsername()
            }

            val displayImageUrl = when {

                isDeleted -> null
                communityComment.member.isDeleted() -> null
                else -> imageUrl
            }

            return CommunityCommentResponse(
                communityComment.getId(),
                communityComment.communityPost.getId(),
                communityComment.parent?.getId(),
                communityComment.replyToCommentId,
                communityComment.replyToMember?.getDisplayUsername(),
                displayUsername,
                displayImageUrl,
                visibleContent,
                communityComment.isEdited,
                isDeleted,
                likeCount,
                communityComment.getCreatedAt(),
                communityComment.getUpdatedAt(),
                replies
            )
        }
    }
}
