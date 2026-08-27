package spring.springserver.domain.community.common.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class CommunityStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    NOT_JOB_POST("NOT_JOB_POST", "구인/구직 게시글이 아닙니다.", HttpStatus.BAD_REQUEST),
    REGION_NOT_SET("REGION_NOT_SET", "내 주변 글을 보려면 프로필에 지역을 먼저 설정해야 합니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
