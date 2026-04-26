package spring.springserver.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import spring.springserver.domain.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_direct_key", columnNames = "direct_chat_key")
        },
        indexes = {
                @Index(name = "idx_chat_room_client", columnList = "client_id"),
                @Index(name = "idx_chat_room_professional", columnList = "professionl_id"),
                @Index(name = "idx_chat_room_direct_key", columnList = "direct_chat_key")
        }
)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Member client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professionl_id", nullable = false)
    private Member professional;

    @Column(name = "direct_chat_key", length = 100, updatable = false)
    private String directChatKey;

    /**
     *  있으면 정렬하기 편해짐(마지막에 올라온 메세지 시간)
     * */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    public ChatRoom(Member client,
                    Member professional) {

        this.client = client;
        this.professional = professional;
        this.directChatKey = generateDirectChatKey(client.getId(), professional.getId());
    }

    public void updateLastMessageMeta(Instant at,
                                      String preview) {

        this.lastMessageAt = at;
        this.lastMessagePreview = preview;
    }

    public static String generateDirectChatKey(Long memberId1,
                                               Long memberId2) {

        long first = Math.min(memberId1, memberId2);
        long second = Math.max(memberId1, memberId2);

        return first + ":" + second;
    }
}
