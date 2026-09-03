package spring.springserver.domain.community.job.data.response

import spring.springserver.domain.community.job.entity.CommunityJobComment
import java.time.LocalDateTime

data class CommunityJobCommentResponse(
    val id: Long?,

    val postId: Long?,

    val username: String,

    val content: String,

    val isEdited: Boolean,

    val likeCount: Long,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(
            communityJobComment: CommunityJobComment,
            likeCount: Long
        ): CommunityJobCommentResponse {

            return CommunityJobCommentResponse(
                communityJobComment.getId(),
                communityJobComment.communityJobPost.getId(),
                communityJobComment.member.username,
                communityJobComment.content,
                communityJobComment.isEdited,
                likeCount,
                communityJobComment.getCreatedAt(),
                communityJobComment.getUpdatedAt(),
            )
        }
    }
}
