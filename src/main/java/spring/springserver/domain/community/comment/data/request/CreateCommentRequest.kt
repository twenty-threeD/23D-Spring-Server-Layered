package spring.springserver.domain.community.comment.data.request

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    val postId: Long,

    @field:NotBlank
    val content: String,
)