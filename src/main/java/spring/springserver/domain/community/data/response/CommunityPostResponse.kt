package spring.springserver.domain.community.data.response

import spring.springserver.domain.community.entity.CommunityPost
import java.time.LocalDateTime

data class CommunityPostResponse(
    val id: Long,

    val username: String,

    val title: String,

    val content: String?,

    val fileUrl: String?,

    val viewCount: Int,

    val isEdited: Boolean,

    val commentCount: Long,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?,
) {
    companion object {

        fun of(communityPost: CommunityPost, commentCount: Long): CommunityPostResponse {

            return CommunityPostResponse(
                communityPost.getId()!!,
                communityPost.username,
                communityPost.title,
                communityPost.content,
                communityPost.fileUrl,
                communityPost.viewCount,
                communityPost.isEdited,
                commentCount,
                communityPost.getCreatedAt(),
                communityPost.getUpdatedAt(),
            )
        }
    }
}
