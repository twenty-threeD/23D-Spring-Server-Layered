package spring.springserver.domain.email.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class EmailException(private val code: String,
                          private val message: String,
                          private val httpStatus: HttpStatus) : StatusCode {

    EMAIL_CANNOT_SEND("EMAIL_CANNOT_SEND", "이메일을 전송할 수 없습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_CANNOT_VERIFY("EMAIL_CANNOT_VERIFY", "이메일을 확인할 수 없습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}