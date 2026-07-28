package spring.springserver.domain.notification.listener

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import spring.springserver.domain.chat.event.ChatMessageSentEvent
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.notification.service.NotificationService
import spring.springserver.domain.notification.data.response.ChatNotificationResponse

@Component
class ChatMessageNotificationListener(
    private val chatService: ChatService,
    private val notificationService: NotificationService
) {

    @EventListener
    fun sendChatNotification(
        event: ChatMessageSentEvent
    ) {

        val receiver = chatService.getOtherParticipantUsername(
            roomId = event.message.roomId!!,
            username = event.senderUsername
        )

        notificationService.sendChatNotification(
            receiverUsername = receiver.username,
            notification = ChatNotificationResponse.of(event.message)
        )
    }
}
