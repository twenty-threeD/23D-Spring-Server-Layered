package spring.springserver.domain.post.category.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class PostCategoryStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    POST_CATEGORY_NOT_FOUND("POST_CATEGORY_NOT_FOUND", "존재하지 않는 용역 카테고리입니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
