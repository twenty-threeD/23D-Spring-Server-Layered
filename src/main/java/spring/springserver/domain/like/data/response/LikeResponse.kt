package spring.springserver.domain.like.data.response

data class LikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): LikeResponse {

            return LikeResponse(message)
        }
    }
}