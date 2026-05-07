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

    return new BaseResponse<>(
            HttpStatus.OK.value(),
            data,
            null
    );
  }

  public static BaseResponse<Void> error(HttpStatus httpStatus,
                                         ErrorResponse errorResponse) {

    return new BaseResponse<>(
            httpStatus.value(),
            null,
            errorResponse
    );
  }
}