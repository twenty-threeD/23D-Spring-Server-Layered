package spring.springserver.domain.post.dto.request

import jakarta.validation.constraints.Size
import spring.springserver.domain.member.entity.Member

data class CreatePostRequest(
    @field:Size(max = 200, message = "title은 200자 이하여야 합니다.")
    val title: String,

    val content: String ?= null,

    val member: Member?= null,
)
