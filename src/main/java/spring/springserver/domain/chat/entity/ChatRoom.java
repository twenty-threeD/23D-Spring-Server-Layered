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
        indexes = {
                @Index(name = "idx_chat_room_member_a", columnList = "member_a_id"),
                @Index(name = "idx_chat_room_member_b", columnList = "member_b_id")
        }
)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_a_id", nullable = false)
    private Member memberA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_b_id", nullable = false)
    private Member memberB;

    /**
     *  있으면 정렬하기 편해짐(마지막에 올라온 메세지 시간)
     * */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    public ChatRoom(Member memberA, Member memberB) {
        this.memberA = memberA;
        this.memberB = memberB;
    }

    public void updateLastMessageMeta(Instant at, String preview) {
        this.lastMessageAt = at;
        this.lastMessagePreview = preview;
    }
}