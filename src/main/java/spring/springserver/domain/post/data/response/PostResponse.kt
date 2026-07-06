package spring.springserver.domain.post.data.response

import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

data class PostResponse(
    val id: Long?,

    val title: String,

    val content: String,

    val viewCount: Int,

    val updatedAt: LocalDateTime?,

    val fileUrls: List<String>,

    val member: Member
) {

    companion object {

        fun of(
            post: Post
        ): PostResponse {

            return PostResponse(
                post.getId(),
                post.title,
                post.content,
                post.viewCount,
                post.updatedAt,
                post.attachments.mapNotNull { it.fileUrl },
                post.member
            )
        }
    }
}
