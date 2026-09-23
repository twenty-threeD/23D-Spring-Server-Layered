package spring.springserver.domain.post.review.controller

import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.post.review.data.request.CreatePostReviewRequest
import spring.springserver.domain.post.review.data.request.UpdatePostReviewRequest
import spring.springserver.domain.post.review.data.response.DeletedPostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewSummaryResponse
import spring.springserver.domain.post.review.service.PostReviewService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post/review")
class PostReviewController(
    private val postReviewService: PostReviewService
) {

    @PostMapping
    fun createReview(
        @Valid @RequestBody createPostReviewRequest: CreatePostReviewRequest
    ): BaseResponse<PostReviewResponse> {

        return BaseResponse.ok(postReviewService.createReview(createPostReviewRequest))
    }

    /**
     * 게시글에서 출발한 계약들의 리뷰 목록.
     */
    @GetMapping
    fun viewReviews(
        @RequestParam postId: Long,
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostReviewResponse>> {

        return BaseResponse.ok(postReviewService.viewReviews(
            postId,
            pageable
        ))
    }

    /**
     * 전문가 한 명이 받은 리뷰 목록. 프로필 화면에서 쓴다.
     */
    @GetMapping("/member")
    fun viewMemberReviews(
        @RequestParam memberId: Long,
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostReviewResponse>> {

        return BaseResponse.ok(postReviewService.viewMemberReviews(
            memberId,
            pageable
        ))
    }

    /**
     * 전문가 한 명의 평점·리뷰 수 집계.
     */
    @GetMapping("/summary")
    fun viewReviewSummary(
        @RequestParam memberId: Long
    ): BaseResponse<PostReviewSummaryResponse> {

        return BaseResponse.ok(postReviewService.viewReviewSummary(memberId))
    }

    @PatchMapping
    fun updateReview(
        @Valid @RequestBody updatePostReviewRequest: UpdatePostReviewRequest
    ): BaseResponse<PostReviewResponse> {

        return BaseResponse.ok(postReviewService.updateReview(updatePostReviewRequest))
    }

    @DeleteMapping
    fun deleteReview(
        @RequestParam reviewId: Long
    ): BaseResponse<DeletedPostReviewResponse> {

        return BaseResponse.ok(postReviewService.deleteReview(reviewId))
    }
}
