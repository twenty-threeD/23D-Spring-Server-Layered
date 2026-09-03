package spring.springserver.domain.notification.data.response

import spring.springserver.domain.chat.data.response.ChatMessageResponse
import java.time.Instant

data class ChatNotificationResponse(
    val roomId: Long,
    val senderUsername: String,
    val senderName: String,
    val message: String,
    val createdAt: Instant
) {

    companion object {

        fun of(
            chatMessage: ChatMessageResponse
        ): ChatNotificationResponse =
            ChatNotificationResponse(
                roomId = chatMessage.roomId!!,
                senderUsername = chatMessage.senderUsername,
                senderName = chatMessage.senderName,
                message = chatMessage.message,
                createdAt = chatMessage.createdAt
            )
    }
}