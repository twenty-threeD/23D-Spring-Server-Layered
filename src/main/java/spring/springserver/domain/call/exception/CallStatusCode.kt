package spring.springserver.domain.call.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class CallStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    CALL_NOT_FOUND("CALL_NOT_FOUND", "통화를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CALL_FORBIDDEN("CALL_FORBIDDEN", "해당 통화에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CALL_ROOM_FORBIDDEN("CALL_ROOM_FORBIDDEN", "해당 채팅방의 참여자가 아닙니다.", HttpStatus.FORBIDDEN),
    CALL_ROOM_NOT_FOUND("CALL_ROOM_NOT_FOUND", "존재하지 않는 채팅방입니다.", HttpStatus.NOT_FOUND),
    CALL_INVALID_STATUS("CALL_INVALID_STATUS", "현재 통화 상태에서는 처리할 수 없는 요청입니다.", HttpStatus.CONFLICT),
    CALL_ALREADY_IN_PROGRESS("CALL_ALREADY_IN_PROGRESS", "이미 진행 중인 통화가 있습니다.", HttpStatus.CONFLICT),
    CALL_SELF_NOT_ALLOWED("CALL_SELF_NOT_ALLOWED", "자기 자신에게는 전화를 걸 수 없습니다.", HttpStatus.BAD_REQUEST),
    CALL_MEMBER_NOT_FOUND("CALL_MEMBER_NOT_FOUND", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    SCREEN_SHARE_OCCUPIED("SCREEN_SHARE_OCCUPIED", "이미 다른 참여자가 화면을 공유하고 있습니다.", HttpStatus.CONFLICT),
    SCREEN_SHARE_NOT_STARTED("SCREEN_SHARE_NOT_STARTED", "진행 중인 화면 공유가 없습니다.", HttpStatus.CONFLICT),
    AGORA_NOT_CONFIGURED("AGORA_NOT_CONFIGURED", "Agora 설정이 완료되지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AGORA_TOKEN_ISSUE_FAILED("AGORA_TOKEN_ISSUE_FAILED", "통화 토큰 발급에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    override fun getCode(): String = code
    override fun getMessage(): String = message
    override fun getHttpStatus(): HttpStatus = httpStatus
}
