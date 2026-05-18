package spring.springserver.domain.community.data.response

import spring.springserver.domain.community.entity.CommunityPost
import java.time.LocalDateTime

data class UpdatePostResponse(
    val postId: Long,

    val title: String,

    val content: String?,

    val fileUrl: String?,

    val isEdited: Boolean,

    val updatedAt: LocalDateTime?,
) {
    companion object {

        fun of(communityPost: CommunityPost): UpdatePostResponse {

            return UpdatePostResponse(
                communityPost.getId()!!,
                communityPost.title,
                communityPost.content,
                communityPost.fileUrl,
                communityPost.isEdited,
                communityPost.getUpdatedAt(),
            )
        }
    }
}
