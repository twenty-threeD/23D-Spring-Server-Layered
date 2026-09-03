package spring.springserver.domain.notification.exception

import org.springframework.http.HttpStatus
import spring.springserver.global.exception.status_code.StatusCode

enum class NotificationStatusCode(
    private val code: String,
    private val message: String,
    private val httpStatus: HttpStatus
): StatusCode {

    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "존재하지 않는 알림입니다.", HttpStatus.NOT_FOUND),
    FORBIDDEN_NOTIFICATION_ACCESS("FORBIDDEN_NOTIFICATION_ACCESS", "본인에게 온 알림만 조회하거나 수정할 수 있습니다.", HttpStatus.FORBIDDEN);

    override fun getCode() = code
    override fun getMessage() = message
    override fun getHttpStatus() = httpStatus
}