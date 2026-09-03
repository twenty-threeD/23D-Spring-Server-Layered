package spring.springserver.domain.community.post.data.response

import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import java.time.LocalDateTime

data class UpdatePostResponse(
    val postId: Long?,

    val title: String,

    val content: String?,

    val category: Category,

    val fileUrl: String?,

    val isEdited: Boolean,

    val updatedAt: LocalDateTime?,
) {
    
    companion object {

        fun of(
            communityPost: CommunityPost
        ): UpdatePostResponse {

            return UpdatePostResponse(
                communityPost.getId(),
                communityPost.title,
                communityPost.content,
                communityPost.category,
                communityPost.fileUrl,
                communityPost.isEdited,
                communityPost.getUpdatedAt(),
            )
        }
    }
}