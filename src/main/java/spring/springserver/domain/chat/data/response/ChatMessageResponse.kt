package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatMessage
import java.time.Instant

data class ChatMessageResponse(
    val messageId: Long?,
    val roomId: Long?,
    val message: String,
    val createdAt: Instant
) {

    companion object {

        fun from(
            chatMessage: ChatMessage
        ): ChatMessageResponse = of(chatMessage)

        fun of(
            chatMessage: ChatMessage
        ): ChatMessageResponse =
            ChatMessageResponse(
                messageId = chatMessage.getId(),
                roomId = chatMessage.room.getId(),
                message = chatMessage.message,
                createdAt = chatMessage.createdAt
            )
    }
}
