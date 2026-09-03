package spring.springserver.domain.post.review.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import spring.springserver.domain.post.review.data.request.CreatePostReviewRequest
import spring.springserver.domain.post.review.data.request.UpdatePostReviewRequest
import spring.springserver.domain.post.review.data.response.DeletedPostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewSummaryResponse

interface PostReviewService {

    fun createReview(
        createPostReviewRequest: CreatePostReviewRequest
    ): PostReviewResponse

    fun viewReviews(
        postId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse>

    fun viewReviewSummary(
        postId: Long
    ): PostReviewSummaryResponse

    fun updateReview(
        updatePostReviewRequest: UpdatePostReviewRequest
    ): PostReviewResponse

    fun deleteReview(
        reviewId: Long
    ): DeletedPostReviewResponse
}