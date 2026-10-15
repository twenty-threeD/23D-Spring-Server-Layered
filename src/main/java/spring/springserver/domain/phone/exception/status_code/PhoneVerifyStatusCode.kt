package spring.springserver.domain.phone.exception.status_code

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PhoneVerifyStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {

    CANNOT_SEND_PHONE_VERIFY_NUMBER("CANNOT_SEND_PHONE_VERIFY_NUMBER", "본인인증 메시지를 보낼 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_VERIFY_PHONE_NUMBER("CANNOT_VERIFY_PHONE_NUMBER", "본인인증을 할 수 없습니다.", HttpStatus.BAD_REQUEST),
    VERIFY_ATTEMPTS_EXCEEDED("VERIFY_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 받아주세요.", HttpStatus.TOO_MANY_REQUESTS),
    SEND_TOO_FREQUENT("SEND_TOO_FREQUENT", "인증번호는 잠시 후 다시 요청할 수 있습니다.", HttpStatus.TOO_MANY_REQUESTS),
    SEND_DAILY_LIMIT_EXCEEDED("SEND_DAILY_LIMIT_EXCEEDED", "오늘 요청할 수 있는 인증번호 발송 횟수를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    SEND_IP_RATE_LIMIT_EXCEEDED("SEND_IP_RATE_LIMIT_EXCEEDED", "요청이 많아 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}