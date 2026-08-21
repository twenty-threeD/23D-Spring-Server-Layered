package spring.springserver.domain.phone.exception.status_code

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PhoneVerifyStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {

    CANNOT_SEND_PHONE_VERIFY_NUMBER("CANNOT_SEND_PHONE_VERIFY_NUMBER", "본인인증 메시지를 보낼 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_VERIFY_PHONE_NUMBER("CANNOT_VERIFY_PHONE_NUMBER", "본인인증을 할 수 없습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}