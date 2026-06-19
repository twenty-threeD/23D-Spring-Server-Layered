package spring.springserver.domain.profile.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class ProfileStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    PROFILE_NOT_FOUND("PROFILE_NOT_FOUND", "프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PROFILE_ALREADY_EXIST("PROFILE_ALREADY_EXIST", "이미 프로필이 존재합니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_CHANGE_NOT_ALLOWED("NICKNAME_CHANGE_NOT_ALLOWED", "닉네임은 최초 설정 또는 변경 후 30일 이후에 변경할 수 있습니다.", HttpStatus.BAD_REQUEST);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}