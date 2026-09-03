package spring.springserver.domain.estimate.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class EstimateStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    ESTIMATE_NOT_FOUND("ESTIMATE_NOT_FOUND", "견적서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ESTIMATE_FORBIDDEN("ESTIMATE_FORBIDDEN", "해당 견적서에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    ESTIMATE_ALREADY_PAID("ESTIMATE_ALREADY_PAID", "결제가 완료된 견적서는 수정하거나 삭제할 수 없습니다.", HttpStatus.CONFLICT),
    ESTIMATE_INVALID_MEMBER("ESTIMATE_INVALID_MEMBER", "클라이언트와 전문가는 서로 달라야 합니다.", HttpStatus.BAD_REQUEST),
    ESTIMATE_AMOUNT_MISMATCH("ESTIMATE_AMOUNT_MISMATCH", "결제 금액이 견적서의 최종 금액과 일치하지 않습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
