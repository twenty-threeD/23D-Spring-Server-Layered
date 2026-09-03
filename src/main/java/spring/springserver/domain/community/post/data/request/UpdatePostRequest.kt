package spring.springserver.domain.community.post.data.request

import jakarta.validation.constraints.NotBlank
import spring.springserver.domain.community.post.entity.Category

data class UpdatePostRequest(
    val postId: Long,

    @field:NotBlank
    val title: String,

    val content: String?,

    val category: Category,

    val fileUrl: String?,
)