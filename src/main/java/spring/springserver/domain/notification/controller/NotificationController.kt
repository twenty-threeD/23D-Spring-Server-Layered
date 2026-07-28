package spring.springserver.domain.notification.controller

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
        principal: Principal
    ): SseEmitter = notificationService.subscribe(principal.name)
}
