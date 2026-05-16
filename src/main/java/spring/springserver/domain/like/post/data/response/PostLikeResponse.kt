package spring.springserver.domain.like.post.data.response

data class PostLikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): PostLikeResponse {

            return PostLikeResponse(message)
        }
    }
}