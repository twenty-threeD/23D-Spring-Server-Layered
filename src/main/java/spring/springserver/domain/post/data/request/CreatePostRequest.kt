package spring.springserver.domain.post.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreatePostRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    val title: String,

    @field:Size(max = 2000, message = "내용은 200자 이하여야 합니다.")
    val content: String ?= null,
)
