package spring.springserver.global.data

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseResponse<T>(
    val status: Int,
    val data: T?,
    val error: ErrorResponse?
) {
    companion object {

        fun <T> ok(
            data: T
        ): BaseResponse<T> {

            return BaseResponse(
                status = HttpStatus.OK.value(),
                data = data,
                error = null
            )
        }

        fun ok(data: Nothing?): BaseResponse<Void> {

            return BaseResponse(
                status = HttpStatus.OK.value(),
                data = null,
                error = null
            )
        }

        fun error(
            httpStatus: HttpStatus,
            errorResponse: ErrorResponse
        ): BaseResponse<Void> {

            return BaseResponse(
                status = httpStatus.value(),
                data = null,
                error = errorResponse
            )
        }
    }
}
