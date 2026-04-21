package spring.springserver.global.exception.exception;

import spring.springserver.global.exception.status_code.StatusCode;
import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

  private final StatusCode statusCode;

  public ApplicationException(StatusCode statusCode) {

    super(statusCode.getMessage());
    this.statusCode = statusCode;
  }

  public ApplicationException(StatusCode statusCode,
                              Throwable throwable) {

    super(statusCode.getMessage(), throwable);
    this.statusCode = statusCode;
  }

  public ApplicationException(StatusCode statusCode,
                              String message) {

    super(message);
    this.statusCode = statusCode;
  }

  public static ApplicationException of(StatusCode statusCode) {

    return new ApplicationException(statusCode);
  }

  public static ApplicationException of(StatusCode statusCode,
                                        Throwable throwable) {

    return new ApplicationException(
            statusCode,
            throwable
    );
  }

  public static ApplicationException of(StatusCode statusCode,
                                        String customMessage) {

    return new ApplicationException(
            statusCode,
            customMessage
    );
  }
}
