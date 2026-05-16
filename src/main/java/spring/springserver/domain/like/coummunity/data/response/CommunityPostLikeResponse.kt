package spring.springserver.domain.like.coummunity.data.response

data class CommunityPostLikeResponse(
    val message: String
) {

    companion object {

        fun of(message: String): CommunityPostLikeResponse {

            return CommunityPostLikeResponse(message)
        }
    }
}

