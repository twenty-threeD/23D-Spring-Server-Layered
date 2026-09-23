package spring.springserver.domain.post.review.data.response

/**
 * 전문가 한 명이 받은 리뷰의 집계. 평점은 사람에게 쌓이므로 게시글이 아닌
 * 회원 기준으로 낸다.
 */
data class PostReviewSummaryResponse(
    val memberId: Long,

    val reviewCount: Long,

    val averageRating: Double,
) {

    companion object {

        fun of(
            memberId: Long,
            reviewCount: Long,
            averageRating: Double
        ): PostReviewSummaryResponse {

            return PostReviewSummaryResponse(
                memberId,
                reviewCount,
                averageRating,
            )
        }
    }
}
