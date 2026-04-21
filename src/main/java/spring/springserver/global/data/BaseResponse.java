package spring.springserver.global.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import spring.springserver.global.exception.status_code.StatusCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T> (
    int status,
    T data,
    ErrorResponse error
) {

  public static <T> BaseResponse<T> ok(T data) {

    return new BaseResponse<>(HttpStatus.OK.value(), data, null);
  }

  public static BaseResponse<Void> error(HttpStatus status, String code, String message) {

    return new BaseResponse<>(status.value(), null, ErrorResponse.of(code, message));
  }

  public static BaseResponse<Void> error(HttpStatus status, String code, String message, Map<String, String> details) {

    return new BaseResponse<>(status.value(), null, ErrorResponse.of(code, message, details));
  }

  public static BaseResponse<Void> error(HttpStatus status, ErrorResponse error) {

    return new BaseResponse<>(status.value(), null, error);
  }

  public static <T> BaseResponse<T> error(StatusCode status) {

    return new BaseResponse<>(status.getHttpStatus().value(), null, ErrorResponse.of(status.getCode(), status.getMessage()));
  }
}