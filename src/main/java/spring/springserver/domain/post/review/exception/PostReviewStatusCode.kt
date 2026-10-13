package spring.springserver.domain.post.review.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PostReviewStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    INVALID_POST_REVIEW("INVALID_POST_REVIEW", "존재하지 않는 리뷰입니다.", HttpStatus.NOT_FOUND),
    INVALID_POST_REVIEW_RATING("INVALID_POST_REVIEW_RATING", "별점은 1점 이상 5점 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    REVIEW_CLIENT_ONLY("REVIEW_CLIENT_ONLY", "계약의 의뢰인(갑)만 리뷰를 남길 수 있습니다.", HttpStatus.FORBIDDEN),
    REVIEW_PAYMENT_REQUIRED("REVIEW_PAYMENT_REQUIRED", "결제가 완료된 계약만 리뷰를 남길 수 있습니다.", HttpStatus.FORBIDDEN),
    ALREADY_REVIEWED_CONTRACT("ALREADY_REVIEWED_CONTRACT", "이미 리뷰를 작성한 계약입니다.", HttpStatus.CONFLICT),
    FORBIDDEN_POST_REVIEW_ACCESS("FORBIDDEN_POST_REVIEW_ACCESS", "본인이 작성한 리뷰만 수정 또는 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    NOT_ENOUGH_REVIEWS_TO_ANALYZE("NOT_ENOUGH_REVIEWS_TO_ANALYZE", "분석할 리뷰가 충분하지 않습니다.", HttpStatus.BAD_REQUEST),
    REVIEW_ANALYSIS_FAILED("REVIEW_ANALYSIS_FAILED", "리뷰 분석에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY),
    REVIEW_ANALYSIS_IN_PROGRESS("REVIEW_ANALYSIS_IN_PROGRESS", "리뷰를 분석하고 있습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.ACCEPTED);

    override fun getCode() = code
    override fun getMessage() = message
    override fun getHttpStatus() = httpStatus
}
