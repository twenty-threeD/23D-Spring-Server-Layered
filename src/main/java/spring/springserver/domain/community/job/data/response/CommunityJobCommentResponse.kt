package spring.springserver.domain.community.job.data.response

import spring.springserver.domain.community.job.entity.CommunityJobComment
import java.time.LocalDateTime

data class CommunityJobCommentResponse(
    val id: Long?,

    val postId: Long?,

    val username: String,

    val content: String,

    val isEdited: Boolean,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(
            communityJobComment: CommunityJobComment
        ): CommunityJobCommentResponse {

            return CommunityJobCommentResponse(
                communityJobComment.getId(),
                communityJobComment.communityJobPost.getId(),
                communityJobComment.member.getDisplayUsername(),
                communityJobComment.content,
                communityJobComment.isEdited,
                communityJobComment.getCreatedAt(),
                communityJobComment.getUpdatedAt(),
            )
        }
    }
}
