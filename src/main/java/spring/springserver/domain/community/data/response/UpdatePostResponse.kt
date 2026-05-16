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
                postId = communityPost.getId()!!,
                title = communityPost.title,
                content = communityPost.content,
                fileUrl = communityPost.fileUrl,
                isEdited = communityPost.isEdited,
                updatedAt = communityPost.getUpdatedAt(),
            )
        }
    }
}
