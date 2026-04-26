package spring.springserver.domain.chat.data.response;

import spring.springserver.domain.chat.entity.ChatMessageDocument;

import java.time.Instant;

public record ChatMessageResponse(

        String messageId,
        Long roomId,
        String senderUsername,
        String senderName,
        String message,
        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessageDocument chatMessageDocument) {

        return new ChatMessageResponse(
                chatMessageDocument.getId(),
                chatMessageDocument.getRoomId(),
                chatMessageDocument.getSenderUsername(),
                chatMessageDocument.getSenderName(),
                chatMessageDocument.getMessage(),
                chatMessageDocument.getCreatedAt()
        );
    }
}
