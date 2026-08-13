package spring.springserver.domain.notification.controller

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.data.response.DeletedNotificationResponse
import spring.springserver.domain.notification.data.response.NotificationResponse
import spring.springserver.domain.notification.data.response.UnreadNotificationCountResponse
import spring.springserver.domain.notification.service.NotificationService
import spring.springserver.global.data.BaseResponse
import java.security.Principal

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
        principal: Principal,
        httpServletResponse: HttpServletResponse,
        @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: Long?
    ): SseEmitter {

        // 프록시(nginx 등)가 SSE 스트림을 gzip 압축하거나 버퍼링하면
        // 이벤트가 즉시 전달되지 않으므로 중간 계층에 변환/버퍼링 금지를 요청한다.
        httpServletResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, no-transform")
        httpServletResponse.setHeader(HttpHeaders.CONTENT_ENCODING, "identity")
        httpServletResponse.setHeader("X-Accel-Buffering", "no")

        return notificationService.subscribe(principal.name, lastEventId)
    }

    @GetMapping
    fun getNotifications(
        principal: Principal
    ): BaseResponse<List<NotificationResponse>> {

        return BaseResponse.ok(notificationService.getNotifications(principal.name))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        principal: Principal
    ): BaseResponse<UnreadNotificationCountResponse> {

        return BaseResponse.ok(notificationService.getUnreadCount(principal.name))
    }

    // 읽은 알림은 보관하지 않으므로 읽음 처리는 곧 삭제다.
    @DeleteMapping("/{notificationId}")
    fun readNotification(
        principal: Principal,
        @PathVariable notificationId: Long
    ): BaseResponse<DeletedNotificationResponse> {

        return BaseResponse.ok(notificationService.readNotification(principal.name, notificationId))
    }

    @DeleteMapping
    fun readAllNotifications(
        principal: Principal
    ): BaseResponse<DeletedNotificationResponse> {

        return BaseResponse.ok(notificationService.readAllNotifications(principal.name))
    }
}