package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatMessage
import java.time.Instant

data class ChatMessageResponse(
    val messageId: Long?,
    val roomId: Long?,
    val senderUsername: String,
    val senderName: String,
    val message: String,
    val createdAt: Instant
) {

    companion object {

        fun of(
            chatMessage: ChatMessage
        ): ChatMessageResponse =
            ChatMessageResponse(
                messageId = chatMessage.getId(),
                roomId = chatMessage.room.getId(),
                senderUsername = chatMessage.sender.username,
                senderName = chatMessage.sender.name,
                message = chatMessage.message,
                createdAt = chatMessage.createdAt
            )
    }
}
