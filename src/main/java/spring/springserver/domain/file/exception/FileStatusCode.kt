package spring.springserver.domain.file.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class FileStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
) : StatusCode {

    FILE_EMPTY("FILE_EMPTY", "업로드할 파일이 비어 있습니다.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    override fun getCode(): String = code

    override fun getMessage(): String = message

    override fun getHttpStatus(): HttpStatus = httpStatus
}
