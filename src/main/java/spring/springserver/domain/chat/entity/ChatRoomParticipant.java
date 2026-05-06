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
        name = "chat_room_participant",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_participant_room_member", columnNames = {"room_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_chat_room_participant_room", columnList = "room_id"),
                @Index(name = "idx_chat_room_participant_member", columnList = "member_id"),
                @Index(name = "idx_chat_room_participant_member_visible", columnList = "member_id, visible")
        }
)
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public ChatRoomParticipant(ChatRoom room,
                               Member member) {

        this.room = room;
        this.member = member;
        this.visible = true;
        this.joinedAt = Instant.now();
    }

    public void leave(Instant deletedAt) {

        this.visible = false;
        this.deletedAt = deletedAt;
    }

    public void reactivate() {

        this.visible = true;
    }
}
