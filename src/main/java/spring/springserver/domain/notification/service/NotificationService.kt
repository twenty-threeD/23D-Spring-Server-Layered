package spring.springserver.domain.notification.service

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.data.response.ChatNotificationResponse

interface NotificationService {

    fun subscribe(
        username: String
    ): SseEmitter

    fun sendChatNotification(
        receiverUsername: String,
        notification: ChatNotificationResponse
    )
}
