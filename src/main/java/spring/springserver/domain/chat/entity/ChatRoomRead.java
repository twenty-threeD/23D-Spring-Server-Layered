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
        name = "chat_room_read",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_room_member",
                        columnNames = {"chat_room_id", "member_id"}
                )
        },
        indexes = {

                @Index(name = "idx_room_read_room", columnList = "chat_room_id"),
                @Index(name = "idx_room_read_member", columnList = "member_id")
        }
)
public class ChatRoomRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 방의 읽음인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // 누가 읽었는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * Redis 메시지용 커서.
     * - room별 INCR seq를 쓰면 long으로 저장 가능
     * - Stream recordId를 쓰면 String으로 저장해야 함
     */
    @Column(name = "last_read_seq", nullable = false)
    private long lastReadSeq;

    @Column(name = "updated_at")
    private Instant updatedAt;

    //어디까지 읽었는지
    public ChatRoomRead(ChatRoom chatRoom,
                        Member member) {

        this.chatRoom = chatRoom;
        this.member = member;
        this.lastReadSeq = 0L;
        this.updatedAt = Instant.now();
    }

    //읽음 처리
    public void markRead(long seq) {

        if (seq > this.lastReadSeq) {

            this.lastReadSeq = seq;
            this.updatedAt = Instant.now();
        }
    }
}