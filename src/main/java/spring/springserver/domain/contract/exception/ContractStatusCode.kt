package spring.springserver.domain.contract.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class ContractStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    CONTRACT_NOT_FOUND("CONTRACT_NOT_FOUND", "계약서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONTRACT_FORBIDDEN("CONTRACT_FORBIDDEN", "해당 계약서에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CONTRACT_INVALID_MEMBER("CONTRACT_INVALID_MEMBER", "의뢰인(갑)과 전문가(을)는 서로 달라야 합니다.", HttpStatus.BAD_REQUEST),
    CONTRACT_INVALID_FILE("CONTRACT_INVALID_FILE", "계약서는 PDF 파일만 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
