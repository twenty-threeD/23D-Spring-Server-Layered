package spring.springserver.domain.community.post.data.response

import spring.springserver.domain.community.post.entity.CommunityPost
import java.time.LocalDateTime

data class CreatePostResponse(
    val postId: Long,

    val username: String,

    val title: String,

    val content: String?,

    val fileUrl: String?,

    val createdAt: LocalDateTime?,
) {
    companion object {

        fun of(communityPost: CommunityPost): CreatePostResponse {

            return CreatePostResponse(
                communityPost.getId()!!,
                communityPost.username,
                communityPost.title,
                communityPost.content,
                communityPost.fileUrl,
                communityPost.getCreatedAt(),
            )
        }
    }
}
