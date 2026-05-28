package spring.springserver.domain.post.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PostStatusCode(private val code: String,
                          private val message: String,
                          private val httpStatus: HttpStatus): StatusCode {

    FORBIDDEN_POST_ACCESS("FORBIDDEN_POST_ACCESS", "본인이 작성한 게시글만 수정 또는 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    INVALID_POST("INVALID_POST", "존재하지 않는 게시글입니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code
    override fun getMessage(): String? = message
    override fun getHttpStatus(): HttpStatus? = httpStatus
}