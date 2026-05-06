package spring.springserver.domain.auth.exception;

import spring.springserver.global.exception.status_code.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthStatusCode implements StatusCode {
  INVALID_JWT("INVALID_JWT", "유효하지 않은 JWT입니다.", HttpStatus.UNAUTHORIZED),
  AVAILABLE_ACCESS_TOKEN("AVAILABLE_ACCESS_TOKEN", "사용 가능한 엑세스 토큰이 존재 합니다.", HttpStatus.UNAUTHORIZED),
  INVALID_CREDENTIALS("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
  ACCOUNT_LOCKED("ACCOUNT_LOCKED", "계정이 비활성화 되어있습니다.", HttpStatus.UNAUTHORIZED),
  ACCOUNT_DISABLED("ACCOUNT_DISABLED", "계정이 비활성화되었습니다.", HttpStatus.UNAUTHORIZED),
  ALREADY_LOGGED_OUT("ALREADY_LOGGED_OUT", "이미 로그아웃 되었습니다.", HttpStatus.BAD_REQUEST),

  USERNAME_NOT_FOUND("USERNAME_NOT_FOUND", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
  USERNAME_ALREADY_EXIST("USERNAME_HAS_TAKEN", "중복된 사용자명입니다.", HttpStatus.BAD_REQUEST),
  EMAIL_ALREADY_EXIST("EMAIL_ALREADY_EXIST", "해당 이메일로 이미 가입된 계정이 존재합니다.", HttpStatus.BAD_REQUEST),
  PHONE_ALREADY_EXIST("PHONE_ALREADY_EXIST", "해당 전화번호로 이미 가입된 계정이 존재합니다.", HttpStatus.BAD_REQUEST),
  COMPANY_ALREADY_EXIST("COMPANY_ALREADY_EXIST", "중복된 사업자 입니다.", HttpStatus.BAD_REQUEST),
  PASSWORD_IS_WEAK("PASSWORD_IS_WEAK", "비밀번호는 8자 이상이며, 영어 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.", HttpStatus.BAD_REQUEST),

  CANNOT_SEND_EMAIL("CANNOT_SEND_EMAIL", "이메일을 전송할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  CANNOT_VERIFY_EMAIL("CANNOT_VERIFY_EMAIL", "이메일을 인증할 수 없습니다", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
