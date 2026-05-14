package spring.springserver.domain.post.dto.response

import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime
import java.util.Optional

data class PostResponse(

    val id: Long?,

    val title: String,

    val content: String?,

    val image_url: String?,

    val view_count: Int,

    val created_at: LocalDateTime?,

    val updated_at: LocalDateTime?,
) {

    companion object {

        fun of(post: Post) : PostResponse {

            return PostResponse(
                post.id,
                post.title,
                post.content,
                post.image_url,
                post.view_count,
                post.created_at,
                post.updated_at
            )
        }
    }
}