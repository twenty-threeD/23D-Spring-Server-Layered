package spring.springserver.domain.post.review.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import spring.springserver.domain.post.review.data.request.CreatePostReviewRequest
import spring.springserver.domain.post.review.data.request.UpdatePostReviewRequest
import spring.springserver.domain.post.review.data.response.DeletedPostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewSummaryResponse
import spring.springserver.domain.post.review.data.response.ReviewableContractResponse

interface PostReviewService {

    fun createReview(
        createPostReviewRequest: CreatePostReviewRequest
    ): PostReviewResponse

    /**
     * 이 게시글에서 로그인 회원이 지금 리뷰를 남길 수 있는 계약 목록.
     * createReview와 같은 조건으로 거르므로 여기 나온 계약은 작성 시 검증을 통과한다.
     */
    fun viewReviewableContracts(
        postId: Long
    ): List<ReviewableContractResponse>

    fun viewReviews(
        postId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse>

    fun viewMemberReviews(
        memberId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse>

    fun viewReviewSummary(
        memberId: Long
    ): PostReviewSummaryResponse

    fun updateReview(
        updatePostReviewRequest: UpdatePostReviewRequest
    ): PostReviewResponse

    fun deleteReview(
        reviewId: Long
    ): DeletedPostReviewResponse
}
