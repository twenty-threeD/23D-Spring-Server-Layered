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
    SELF_POST_REVIEW_NOT_ALLOWED("SELF_POST_REVIEW_NOT_ALLOWED", "본인이 작성한 게시글에는 리뷰를 남길 수 없습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_REVIEWED_POST("ALREADY_REVIEWED_POST", "이미 리뷰를 작성한 게시글입니다.", HttpStatus.CONFLICT),
    FORBIDDEN_POST_REVIEW_ACCESS("FORBIDDEN_POST_REVIEW_ACCESS", "본인이 작성한 리뷰만 수정 또는 삭제할 수 있습니다.", HttpStatus.FORBIDDEN);

    override fun getCode() = code
    override fun getMessage() = message
    override fun getHttpStatus() = httpStatus
}