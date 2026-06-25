package spring.springserver.domain.post.favorite.data.response

data class PostFavoriteResponse(
    val postId: Long,

    val favoriteCount: Long,

    val message: String,
) {

    companion object {

        fun of(
            postId: Long,
            favoriteCount: Long,
            message: String
        ): PostFavoriteResponse {

            return PostFavoriteResponse(
                postId,
                favoriteCount,
                message,
            )
        }
    }
}
