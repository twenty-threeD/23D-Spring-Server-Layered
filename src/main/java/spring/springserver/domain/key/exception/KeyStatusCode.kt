package spring.springserver.domain.key.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class KeyStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    PRIVATE_KEY_DECRYPTION_FAILED("PRIVATE_KEY_DECRYPTION_FAILED", "서명 키를 불러오지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}