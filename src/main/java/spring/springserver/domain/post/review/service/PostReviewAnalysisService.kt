package spring.springserver.domain.post.review.service

import spring.springserver.domain.post.review.data.response.PostReviewAnalysisResponse

interface PostReviewAnalysisService {

    fun viewReviewAnalysis(
        postId: Long
    ): PostReviewAnalysisResponse
}
