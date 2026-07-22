package spring.springserver.global.data

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
    val details: Map<String, String>? = null
) {
    companion object {
        fun of(code: String, message: String): ErrorResponse {
            return ErrorResponse(
                code = code,
                message = message,
                timestamp = LocalDateTime.now()
            )
        }

        fun of(
            code: String,
            message: String,
            details: Map<String, String>
        ): ErrorResponse {
            return ErrorResponse(
                code = code,
                message = message,
                timestamp = LocalDateTime.now(),
                details = details
            )
        }
    }
}
