package spring.springserver.domain.post.review.data.response

data class PostReviewSummaryResponse(
    val postId: Long,

    val reviewCount: Long,

    val averageRating: Double,
) {

    companion object {

        fun of(
            postId: Long,
            reviewCount: Long,
            averageRating: Double
        ): PostReviewSummaryResponse {

            return PostReviewSummaryResponse(
                postId,
                reviewCount,
                averageRating,
            )
        }
    }
}