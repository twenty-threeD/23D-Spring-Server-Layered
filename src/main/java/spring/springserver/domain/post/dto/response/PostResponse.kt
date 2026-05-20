package spring.springserver.domain.post.dto.response

import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime
import java.util.Optional

data class PostResponse(

    val id: Long?,

    val title: String,

    val content: String?,

    val ViewCount: Int,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(post: Post) : PostResponse {

            return PostResponse(
                post.id,
                post.title,
                post.content,
                post.viewCount,
                post.updatedAt
            )
        }
    }
}