package spring.springserver.domain.like.coummunity.data.response

data class CommunityPostUnlikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): CommunityPostUnlikeResponse {

            return CommunityPostUnlikeResponse(message)
        }
    }
}
