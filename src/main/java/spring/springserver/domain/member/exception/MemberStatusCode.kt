package spring.springserver.domain.member.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class MemberStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PHONE_NOT_VERIFIED("PHONE_NOT_VERIFIED", "휴대폰 본인인증이 필요합니다.", HttpStatus.FORBIDDEN),
    SOCIAL_ACCOUNT_CANNOT_CHANGE("SOCIAL_ACCOUNT_CANNOT_CHANGE", "소셜 로그인 계정은 이메일·전화번호를 변경할 수 없습니다.", HttpStatus.FORBIDDEN),
    SOCIAL_ACCOUNT_CANNOT_RESET_PASSWORD("SOCIAL_ACCOUNT_CANNOT_RESET_PASSWORD", "소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.", HttpStatus.FORBIDDEN),
    VERIFICATION_REQUIRED("VERIFICATION_REQUIRED", "휴대폰 또는 이메일 인증이 필요합니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_TARGET_MISMATCH("VERIFICATION_TARGET_MISMATCH", "본인 확인 정보가 계정 정보와 일치하지 않습니다.", HttpStatus.FORBIDDEN);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}