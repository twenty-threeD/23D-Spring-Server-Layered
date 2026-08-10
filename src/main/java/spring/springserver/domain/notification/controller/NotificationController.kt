package spring.springserver.domain.notification.controller

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.service.NotificationService
import java.security.Principal

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
        principal: Principal,
        httpServletResponse: HttpServletResponse
    ): SseEmitter {

        // 프록시(nginx 등)가 SSE 스트림을 gzip 압축하거나 버퍼링하면
        // 이벤트가 즉시 전달되지 않으므로 중간 계층에 변환/버퍼링 금지를 요청한다.
        httpServletResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, no-transform")
        httpServletResponse.setHeader(HttpHeaders.CONTENT_ENCODING, "identity")
        httpServletResponse.setHeader("X-Accel-Buffering", "no")

        return notificationService.subscribe(principal.name)
    }
}
