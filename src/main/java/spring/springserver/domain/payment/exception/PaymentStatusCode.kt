package spring.springserver.domain.payment.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PaymentStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {

    TOSS_PAYMENTS_SECRET_KEY_MISSING(
        "TOSS_PAYMENTS_SECRET_KEY_MISSING",
        "토스페이먼츠 시크릿 키가 설정되지 않았습니다.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    TOSS_PAYMENTS_REQUEST_INVALID(
        "TOSS_PAYMENTS_REQUEST_INVALID",
        "토스페이먼츠 요청이 유효하지 않습니다.",
        HttpStatus.BAD_REQUEST
    ),
    TOSS_PAYMENTS_REQUEST_FAILED(
        "TOSS_PAYMENTS_REQUEST_FAILED",
        "토스페이먼츠 요청에 실패했습니다.",
        HttpStatus.BAD_GATEWAY
    );

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
