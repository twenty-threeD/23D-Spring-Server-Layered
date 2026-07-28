package spring.springserver.global.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.data.ErrorResponse

@Component
class ApiAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {

        val error = ErrorResponse.of(
            AuthStatusCode.INVALID_JWT.getCode(),
            AuthStatusCode.INVALID_JWT.getMessage()
        )

        response.status = AuthStatusCode.INVALID_JWT.getHttpStatus().value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                BaseResponse.error(
                    AuthStatusCode.INVALID_JWT.getHttpStatus(),
                    error
                )
            )
        )
    }
}
