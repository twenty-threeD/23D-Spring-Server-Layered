package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatMessage
import java.time.Instant

data class ChatMessageResponse(
    val messageId: Long?,
    val roomId: Long?,
    val senderUsername: String,
    val senderName: String,
    val message: String,
    val createdAt: Instant,
    val attachedFileUrls: List<String>,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val payment: ChatPaymentResponse? = null
) {

    companion object {

        /**
         * DB에서 복원한 메시지. 결제 상세는 엔티티에 저장된 JSON을 역직렬화해 호출부가 넘긴다.
         */
        fun of(
            chatMessage: ChatMessage,
            payment: ChatPaymentResponse?
        ): ChatMessageResponse =
            ChatMessageResponse(
                messageId = chatMessage.getId(),
                roomId = chatMessage.room.getId(),
                senderUsername = chatMessage.sender.username,
                senderName = chatMessage.sender.name,
                message = chatMessage.message,
                createdAt = chatMessage.createdAt,
                attachedFileUrls = chatMessage.attachmentUrls(),
                type = chatMessage.messageType,
                payment = payment
            )
    }
}
