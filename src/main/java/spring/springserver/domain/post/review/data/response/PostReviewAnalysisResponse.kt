package spring.springserver.domain.post.review.data.response

import java.time.LocalDateTime

/**
 * 게시글 한 건에 달린 리뷰의 AI 분석 결과.
 */
data class PostReviewAnalysisResponse(
    val postId: Long,

    val summary: String,

    val keywords: List<String>,

    val analyzedReviewCount: Long,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(
            postId: Long,
            summary: String,
            keywords: List<String>,
            analyzedReviewCount: Long,
            updatedAt: LocalDateTime?
        ): PostReviewAnalysisResponse {

            return PostReviewAnalysisResponse(
                postId,
                summary,
                keywords,
                analyzedReviewCount,
                updatedAt,
            )
        }
    }
}
