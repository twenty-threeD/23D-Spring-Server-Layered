package spring.springserver.domain.post.dto.request

data class CreatePostRequest(

    val title: String,
    val content: String ?=null,
    val image_url: String ?= null,
)
