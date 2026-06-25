package spring.springserver.domain.chat.data.response

import java.time.Instant

data class ChatMessageResponse(
    val messageId: Long?,
    val roomId: Long?,
    val senderUsername: String,
    val senderName: String,
    val message: String,
    val createdAt: Instant,
    val attachedFileUrls: List<String>
)
