package spring.springserver.domain.community.common.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class CommunityStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    REGION_NOT_SET("REGION_NOT_SET", "내 주변 글을 보려면 프로필에 지역을 먼저 설정해야 합니다.", HttpStatus.BAD_REQUEST),
    NEIGHBORHOOD_LOGIN_REQUIRED("NEIGHBORHOOD_LOGIN_REQUIRED", "동네 주민 글은 로그인 후 볼 수 있습니다.", HttpStatus.UNAUTHORIZED),
    COMMENT_PARENT_NOT_FOUND("COMMENT_PARENT_NOT_FOUND", "원댓글을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMENT_PARENT_POST_MISMATCH("COMMENT_PARENT_POST_MISMATCH", "원댓글이 다른 게시글의 댓글입니다.", HttpStatus.BAD_REQUEST),
    COMMENT_REPLY_DEPTH_EXCEEDED("COMMENT_REPLY_DEPTH_EXCEEDED", "답글의 답글은 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMENT_REPLY_TARGET_INVALID("COMMENT_REPLY_TARGET_INVALID", "답글 대상 댓글이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
