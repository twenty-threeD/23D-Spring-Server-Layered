package spring.springserver.domain.email.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class EmailException(private val code: String,
                          private val message: String,
                          private val httpStatus: HttpStatus) : StatusCode {

    EMAIL_CANNOT_SEND("EMAIL_CANNOT_SEND", "이메일을 전송할 수 없습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_CANNOT_VERIFY("EMAIL_CANNOT_VERIFY", "이메일을 확인할 수 없습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFY_ATTEMPTS_EXCEEDED("EMAIL_VERIFY_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 인증코드를 다시 받아주세요.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_SEND_TOO_FREQUENT("EMAIL_SEND_TOO_FREQUENT", "인증코드는 잠시 후 다시 요청할 수 있습니다.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_SEND_LIMIT_EXCEEDED("EMAIL_SEND_LIMIT_EXCEEDED", "인증코드 발송 한도를 초과했습니다. 나중에 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}