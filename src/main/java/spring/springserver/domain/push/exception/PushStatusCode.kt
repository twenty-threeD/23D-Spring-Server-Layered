package spring.springserver.domain.push.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PushStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    PUSH_MEMBER_NOT_FOUND("PUSH_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    override fun getCode() = code
    override fun getMessage() = message
    override fun getHttpStatus() = httpStatus
}
