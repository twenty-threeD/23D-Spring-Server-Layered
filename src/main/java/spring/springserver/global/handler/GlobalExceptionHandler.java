package spring.springserver.global.handler;

import spring.springserver.domain.auth.exception.AuthStatusCode;
import spring.springserver.global.data.BaseResponse;
import spring.springserver.global.data.ErrorResponse;
import spring.springserver.global.exception.exception.ApplicationException;
import spring.springserver.global.exception.status_code.CommonStatusCode;
import spring.springserver.global.exception.status_code.StatusCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<BaseResponse<Void>> handleApplicationException(ApplicationException ex) {
    StatusCode statusCode = ex.getStatusCode();
    ErrorResponse error = ErrorResponse.of(
        statusCode.getCode(),
        ex.getMessage() != null ? ex.getMessage() : statusCode.getMessage()
    );
    return ResponseEntity
        .status(statusCode.getHttpStatus())
        .body(BaseResponse.error(statusCode.getHttpStatus(), error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> details = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String field = ((FieldError) error).getField();
      String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "잘못된 입력값 입니다.";
      details.put(field, message);
    });

    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        "요청값이 유효하지 않습니다.",
        details
    );

    return ResponseEntity
        .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(), error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
    Map<String, String> details = new HashMap<>();
    ex.getConstraintViolations().forEach(violation -> {
      String fieldName = violation.getPropertyPath().toString();
      String message = violation.getMessage();
      details.put(fieldName, message);
    });

    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        "요청값이 유효하지 않습니다.",
        details
    );

    return ResponseEntity
        .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(), error));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        "필수 파라미터가 누락되었습니다: " + ex.getParameterName()
    );

    return ResponseEntity
        .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(), error));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        "파라미터 타입이 잘못되었습니다: " + ex.getName()
    );

    return ResponseEntity
        .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(), error));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INVALID_ARGUMENT.getCode(),
        ex.getMessage() != null ? ex.getMessage() : "잘못된 요청 파라미터입니다."
    );

    return ResponseEntity
        .status(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INVALID_ARGUMENT.getHttpStatus(), error));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<BaseResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
    ErrorResponse error = ErrorResponse.of(
        AuthStatusCode.INVALID_CREDENTIALS.getCode(),
        AuthStatusCode.INVALID_CREDENTIALS.getMessage()
    );
    return ResponseEntity
        .status(AuthStatusCode.INVALID_CREDENTIALS.getHttpStatus())
        .body(BaseResponse.error(AuthStatusCode.INVALID_CREDENTIALS.getHttpStatus(), error));
  }

  @ExceptionHandler(LockedException.class)
  public ResponseEntity<BaseResponse<Void>> handleAccountLockedException(LockedException ex) {
    ErrorResponse error = ErrorResponse.of(
        AuthStatusCode.ACCOUNT_LOCKED.getCode(),
        AuthStatusCode.ACCOUNT_LOCKED.getMessage()
    );
    return ResponseEntity
        .status(AuthStatusCode.ACCOUNT_LOCKED.getHttpStatus())
        .body(BaseResponse.error(AuthStatusCode.ACCOUNT_LOCKED.getHttpStatus(), error));
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<BaseResponse<Void>> handleAccountDisabledException(DisabledException ex) {
    ErrorResponse error = ErrorResponse.of(
        AuthStatusCode.ACCOUNT_DISABLED.getCode(),
        AuthStatusCode.ACCOUNT_DISABLED.getMessage()
    );
    return ResponseEntity
        .status(AuthStatusCode.ACCOUNT_DISABLED.getHttpStatus())
        .body(BaseResponse.error(AuthStatusCode.ACCOUNT_DISABLED.getHttpStatus(), error));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.ENDPOINT_NOT_FOUND.getCode(),
        CommonStatusCode.ENDPOINT_NOT_FOUND.getMessage()
    );
    return ResponseEntity
        .status(CommonStatusCode.ENDPOINT_NOT_FOUND.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.ENDPOINT_NOT_FOUND.getHttpStatus(), error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
    log.error("요청 처리 중 에러 발생: {}", ex.getMessage());
    ErrorResponse error = ErrorResponse.of(
        CommonStatusCode.INTERNAL_SERVER_ERROR.getCode(),
        CommonStatusCode.INTERNAL_SERVER_ERROR.getMessage()
    );
    return ResponseEntity
        .status(CommonStatusCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(BaseResponse.error(CommonStatusCode.INTERNAL_SERVER_ERROR.getHttpStatus(), error));
  }
}
