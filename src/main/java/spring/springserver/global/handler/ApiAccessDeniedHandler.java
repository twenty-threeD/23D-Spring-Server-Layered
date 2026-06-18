package spring.springserver.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import spring.springserver.global.data.BaseResponse;
import spring.springserver.global.data.ErrorResponse;
import spring.springserver.global.exception.status_code.CommonStatusCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request,
                     HttpServletResponse response,
                     AccessDeniedException accessDeniedException) throws IOException {

    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        "접근 권한이 없습니다."
    );

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(
        objectMapper.writeValueAsString(
            BaseResponse.error(org.springframework.http.HttpStatus.FORBIDDEN, error)
        )
    );
  }
}
