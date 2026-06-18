package spring.springserver.domain.location.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class LocationStatusCode(private val code: String,
                              private val message: String,
                              private val httpStatus: HttpStatus): StatusCode {

    SIDO_NOT_FOUND("SIDO_NOT_FOUND", "존재하지 않는 시/도입니다.", HttpStatus.NOT_FOUND),
    SIGUNGU_NOT_FOUND("SIGUNGU_NOT_FOUND", "존재하지 않는 시/군/구입니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}