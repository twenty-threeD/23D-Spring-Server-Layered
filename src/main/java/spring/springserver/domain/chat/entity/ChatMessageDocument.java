package spring.springserver.domain.chat.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@CompoundIndex(name = "idx_room_created_at", def = "{'roomId': 1, 'createdAt': 1}")
public class ChatMessageDocument {

    @Id
    private String id;

    private Long roomId;

    private Long senderId;

    private String senderUsername;

    private String senderName;

    private String message;

    private Instant createdAt;

    public ChatMessageDocument(Long roomId,
                               Long senderId,
                               String senderUsername,
                               String senderName,
                               String message,
                               Instant createdAt) {

        this.roomId = roomId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
    }
}
