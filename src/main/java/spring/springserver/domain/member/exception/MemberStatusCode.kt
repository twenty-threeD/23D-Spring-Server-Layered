package spring.springserver.domain.member.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class MemberStatusCode(private val code: String,
                            private val message: String,
                            private val httpStatus: HttpStatus): StatusCode {

    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}