package spring.springserver.global.exception.status_code

import org.springframework.http.HttpStatus

enum class CommonStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", "유효하지 않은 인자입니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버에 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ENDPOINT_NOT_FOUND("ENDPOINT_NOT_FOUND", "존재하지 않는 엔드포인트입니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code

    override fun getMessage(): String = message

    override fun getHttpStatus(): HttpStatus = httpStatus
}
