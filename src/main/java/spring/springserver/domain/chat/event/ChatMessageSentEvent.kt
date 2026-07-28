package spring.springserver.domain.chat.event

import spring.springserver.domain.chat.data.response.ChatMessageResponse

data class ChatMessageSentEvent(
    val senderUsername: String,
    val message: ChatMessageResponse
)
