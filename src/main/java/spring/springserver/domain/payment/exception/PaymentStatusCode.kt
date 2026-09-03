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
    ),
    PAYMENT_NOT_FOUND(
        "PAYMENT_NOT_FOUND",
        "결제 정보를 찾을 수 없습니다.",
        HttpStatus.NOT_FOUND
    ),
    PAYMENT_ORDER_ID_DUPLICATED(
        "PAYMENT_ORDER_ID_DUPLICATED",
        "이미 사용된 주문번호입니다.",
        HttpStatus.CONFLICT
    ),
    PAYMENT_AMOUNT_MISMATCH(
        "PAYMENT_AMOUNT_MISMATCH",
        "결제 금액이 일치하지 않습니다.",
        HttpStatus.BAD_REQUEST
    ),
    PAYMENT_MEMBER_MISMATCH(
        "PAYMENT_MEMBER_MISMATCH",
        "결제를 요청한 회원이 아닙니다.",
        HttpStatus.FORBIDDEN
    ),
    PAYMENT_ALREADY_PROCESSED(
        "PAYMENT_ALREADY_PROCESSED",
        "이미 처리된 결제입니다.",
        HttpStatus.CONFLICT
    ),
    PAYMENT_BLOCKCHAIN_RECORD_FAILED(
        "PAYMENT_BLOCKCHAIN_RECORD_FAILED",
        "블록체인 기록에 실패하여 결제가 취소되었습니다.",
        HttpStatus.BAD_GATEWAY
    ),
    PAYMENT_CANCEL_FAILED(
        "PAYMENT_CANCEL_FAILED",
        "결제 취소에 실패했습니다. 고객센터로 문의해주세요.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    PAYMENT_NOT_RECORDED_ON_CHAIN(
        "PAYMENT_NOT_RECORDED_ON_CHAIN",
        "블록체인에 기록되지 않은 결제입니다.",
        HttpStatus.NOT_FOUND
    ),
    PAYMENT_CHAT_ROOM_FORBIDDEN(
        "PAYMENT_CHAT_ROOM_FORBIDDEN",
        "참여하지 않은 채팅입니다.",
        HttpStatus.FORBIDDEN
    );

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
