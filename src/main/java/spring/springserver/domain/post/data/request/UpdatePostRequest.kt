package spring.springserver.domain.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    val id: Long,

    @NotBlank
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    val title: String,

    @field:Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    val content: String ?= null,
)
