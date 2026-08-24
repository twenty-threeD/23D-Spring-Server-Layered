package spring.springserver.domain.community.community.post.data.request

import jakarta.validation.constraints.NotBlank

data class UpdatePostRequest(
    val postId: Long,

    @field:NotBlank
    val title: String,

    val content: String?,

    val fileUrl: String?,
)