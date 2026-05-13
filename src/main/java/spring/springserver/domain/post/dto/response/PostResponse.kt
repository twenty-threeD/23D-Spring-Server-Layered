package spring.springserver.domain.post.dto.response

import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.entity.Status
import java.time.LocalDateTime

data class PostResponse(

    val id: Long?,

    val title: String,
    val content: String?,

    val image_url: String?,

    val status: Status,

    val view_count: Int,

    val created_at: LocalDateTime?,
    val updated_at: LocalDateTime?,
) {

    companion object {

        fun of (post: Post) : PostResponse {

            return PostResponse(

                post.id,
                post.title,
                post.content,
                post.image_url,
                post.status,
                post.view_count,
                post.created_at,
                post.updated_at
            )
        }
    }
}