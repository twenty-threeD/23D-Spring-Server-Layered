package spring.springserver.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import spring.springserver.domain.chat.data.request.SendChatMessageRequest;
import spring.springserver.domain.chat.data.response.ChatMessageResponse;
import spring.springserver.domain.chat.service.ChatService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid SendChatMessageRequest sendChatMessageRequest,
                            Principal principal) {

        ChatMessageResponse response = chatService.sendMessage(principal.getName(), sendChatMessageRequest);

        messagingTemplate.convertAndSend("/topic/chat/rooms/" + response.roomId(), response);
    }
}
