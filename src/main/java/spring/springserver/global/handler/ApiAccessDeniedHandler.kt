package spring.springserver.global.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.data.ErrorResponse
import spring.springserver.global.exception.status_code.CommonStatusCode

@Component
class ApiAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val error = ErrorResponse.of(
            CommonStatusCode.INVALID_ARGUMENT.getCode(),
            "접근 권한이 없습니다."
        )

        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                BaseResponse.error(
                    HttpStatus.FORBIDDEN,
                    error
                )
            )
        )
    }
}
