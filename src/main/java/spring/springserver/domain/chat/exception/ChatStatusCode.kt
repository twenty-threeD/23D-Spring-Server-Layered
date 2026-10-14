package spring.springserver.domain.chat.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class ChatStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    TRADE_STATUS_NOT_ALLOWED("TRADE_STATUS_NOT_ALLOWED", "직접 바꿀 수 있는 거래 상태는 취소(CANCELED)뿐입니다.", HttpStatus.BAD_REQUEST),
    TRADE_NOT_CANCELABLE("TRADE_NOT_CANCELABLE", "진행 중인 거래만 취소할 수 있습니다. 결제가 끝난 거래는 결제 취소로 되돌려 주세요.", HttpStatus.CONFLICT),
    NOT_POST_AUTHOR_ROOM("NOT_POST_AUTHOR_ROOM", "채팅 참여자 중 한 명은 게시글 작성자여야 합니다.", HttpStatus.FORBIDDEN);

    override fun getCode(): String = code

    override fun getMessage(): String = message

    override fun getHttpStatus(): HttpStatus = httpStatus
}