package spring.springserver.domain.community.data.response

import spring.springserver.domain.community.entity.CommunityPost
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
                postId = communityPost.getId()!!,
                username = communityPost.username,
                title = communityPost.title,
                content = communityPost.content,
                fileUrl = communityPost.fileUrl,
                createdAt = communityPost.getCreatedAt(),
            )
        }
    }
}
