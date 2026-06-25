package spring.springserver.domain.chat.controller

import jakarta.validation.Valid
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.service.ChatService
import java.security.Principal

@Controller
class ChatMessageController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    @MessageMapping("/chat.send")
    fun sendMessage(
        @Valid sendChatMessageRequest: SendChatMessageRequest,
        principal: Principal
    ) {

        val response = chatService.sendMessage(
            senderUsername = principal.name,
            sendChatMessageRequest = sendChatMessageRequest
        )

        messagingTemplate.convertAndSend(
            "/topic/chat/rooms/${response.roomId}",
            response
        )
    }
}
