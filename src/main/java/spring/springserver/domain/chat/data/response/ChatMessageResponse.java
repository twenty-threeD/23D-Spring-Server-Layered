package spring.springserver.domain.chat.data.response;

import spring.springserver.domain.chat.entity.ChatMessage;

import java.time.Instant;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        String senderUsername,
        String senderName,
        String message,
        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage,
                                           Long roomId) {

        return new ChatMessageResponse(
                chatMessage.getId(),
                roomId,
                chatMessage.getMember().getUsername(),
                chatMessage.getMember().getName(),
                chatMessage.getMessage(),
                chatMessage.getCreatedAt()
        );
    }
}
