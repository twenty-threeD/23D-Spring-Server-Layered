package spring.springserver.domain.community.data.request

import jakarta.validation.constraints.NotBlank

data class UpdateCommentRequest(
    val commentId: Long,

    @field:NotBlank
    val content: String,
)
