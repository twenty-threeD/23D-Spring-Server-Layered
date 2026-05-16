package spring.springserver.domain.like.post.data.response

data class PostUnlikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): PostUnlikeResponse {

            return PostUnlikeResponse(message)
        }
    }
}
