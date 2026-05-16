package spring.springserver.domain.chat.data.response;

import java.time.Instant;
import spring.springserver.domain.chat.entity.ChatMessage;

public record ChatMessageResponse(

        Long messageId,

        Long roomId,

        String message,

        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {

        return of(chatMessage);
    }

    public static ChatMessageResponse of(ChatMessage chatMessage) {

        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRoom().getId(),
                chatMessage.getMessage(),
                chatMessage.getCreatedAt()
        );
    }
}
