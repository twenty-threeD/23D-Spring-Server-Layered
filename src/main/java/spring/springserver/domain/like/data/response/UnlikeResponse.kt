package spring.springserver.domain.like.data.response

data class UnlikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): UnlikeResponse {

            return UnlikeResponse(message)
        }
    }
}
