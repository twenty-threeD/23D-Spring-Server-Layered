package spring.springserver.domain.post.dto.request

import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    val title: String,

    @field:Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    val content: String ?= null,
)
