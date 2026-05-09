package spring.springserver.domain.like.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class LikeStatusCode(private val code: String,
                          private val message: String,
                          private val httpStatus: HttpStatus): StatusCode {

    ALREADY_LIKED("ALREADY_LIKED", "이미 좋아요를 눌렀습니다.", HttpStatus.BAD_REQUEST),
    NOT_LIKED("NOT_LIKED", "좋아요를 누르지않았습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}