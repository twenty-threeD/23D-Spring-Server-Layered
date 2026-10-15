package spring.springserver.domain.post.review.client

/**
 * Claude가 돌려준 분석 결과. 엔티티·응답 DTO와 분리해 클라이언트 경계에서만 쓴다.
 */
data class ReviewAnalysisResult(
    val summary: String,

    val keywords: List<String>,
)
