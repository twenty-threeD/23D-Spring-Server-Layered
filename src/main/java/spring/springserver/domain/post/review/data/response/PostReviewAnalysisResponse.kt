package spring.springserver.domain.post.review.data.response

import java.time.LocalDateTime

/**
 * 전문가 한 명이 받은 리뷰의 AI 분석 결과.
 */
data class PostReviewAnalysisResponse(
    val memberId: Long,

    val summary: String,

    val keywords: List<String>,

    val analyzedReviewCount: Long,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(
            memberId: Long,
            summary: String,
            keywords: List<String>,
            analyzedReviewCount: Long,
            updatedAt: LocalDateTime?
        ): PostReviewAnalysisResponse {

            return PostReviewAnalysisResponse(
                memberId,
                summary,
                keywords,
                analyzedReviewCount,
                updatedAt,
            )
        }
    }
}
