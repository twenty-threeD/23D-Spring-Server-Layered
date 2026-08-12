package spring.springserver.domain.post.review.controller

import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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

    @GetMapping
    fun viewReviews(
        @RequestParam postId: Long,
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostReviewResponse>> {

        return BaseResponse.ok(postReviewService.viewReviews(postId, pageable))
    }

    @GetMapping("/summary")
    fun viewReviewSummary(
        @RequestParam postId: Long
    ): BaseResponse<PostReviewSummaryResponse> {

        return BaseResponse.ok(postReviewService.viewReviewSummary(postId))
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