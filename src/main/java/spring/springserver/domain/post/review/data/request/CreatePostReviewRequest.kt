package spring.springserver.domain.post.review.data.request

import jakarta.validation.constraints.*

data class CreatePostReviewRequest(
    /**
     * 리뷰 대상 계약 아이디. 리뷰는 계약 한 건당 하나만 남길 수 있고,
     * 계약의 의뢰인(갑)만 작성할 수 있다.
     */
    @field:Positive
    val contractId: Long,

    @field:Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @field:Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    val rating: Int,

    @field:NotBlank
    @field:Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다.")
    val content: String,
)
