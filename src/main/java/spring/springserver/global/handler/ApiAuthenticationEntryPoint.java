package spring.springserver.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.global.data.BaseResponse;
import spring.springserver.global.data.ErrorResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(HttpServletRequest request,
                       HttpServletResponse response,
                       AuthenticationException authException) throws IOException {

    ErrorResponse error = ErrorResponse.of(
        AuthStatusCode.INVALID_JWT.getCode(),
        AuthStatusCode.INVALID_JWT.getMessage()
    );

    response.setStatus(AuthStatusCode.INVALID_JWT.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
        objectMapper.writeValueAsString(
            BaseResponse.error(AuthStatusCode.INVALID_JWT.getHttpStatus(), error)
        )
    );
  }
}
