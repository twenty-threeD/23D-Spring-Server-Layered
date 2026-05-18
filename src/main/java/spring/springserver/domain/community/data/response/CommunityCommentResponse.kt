package spring.springserver.domain.community.data.response

import spring.springserver.domain.community.entity.CommunityComment
import java.time.LocalDateTime

data class CommunityCommentResponse(
    val id: Long,

    val postId: Long,

    val username: String,

    val content: String,

    val isEdited: Boolean,

    val likeCount: Long,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?,
) {
    companion object {

        fun of(communityComment: CommunityComment, likeCount: Long): CommunityCommentResponse {

            return CommunityCommentResponse(
                communityComment.getId()!!,
                communityComment.communityPost.getId()!!,
                communityComment.member.username,
                communityComment.content,
                communityComment.isEdited,
                likeCount,
                communityComment.getCreatedAt(),
                communityComment.getUpdatedAt(),
            )
        }
    }
}
