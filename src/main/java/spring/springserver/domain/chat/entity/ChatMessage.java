package spring.springserver.domain.chat.entity;

import jakarta.persistence.*;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.springserver.domain.member.entity.Member;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_message",
        indexes = {
                @Index(name = "idx_chat_message_room_created_at", columnList = "chat_room_id, created_at"),
                @Index(name = "idx_chat_message_sender", columnList = "sender_member_id")
        }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "chat_room_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ChatRoom room;

    @JoinColumn(name = "sender_member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member sender;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
