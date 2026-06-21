package spring.springserver.domain.jobcategory.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class JobCategoryStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    JOB_CATEGORY_NOT_FOUND("JOB_CATEGORY_NOT_FOUND", "존재하지 않는 업종입니다.", HttpStatus.NOT_FOUND);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}