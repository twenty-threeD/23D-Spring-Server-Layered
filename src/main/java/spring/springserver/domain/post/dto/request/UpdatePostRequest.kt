package spring.springserver.domain.post.dto.request

import spring.springserver.domain.post.entity.Status

data class UpdatePostRequest(
    val title: String,
    val content: String ?= null,
    val image_url: String ?= null,
    val status: Status
)
