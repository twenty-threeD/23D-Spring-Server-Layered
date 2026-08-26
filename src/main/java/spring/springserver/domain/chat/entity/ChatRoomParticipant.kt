package spring.springserver.domain.chat.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member
import java.time.Instant

@Entity
@Table(
    name = "chat_room_participant",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_chat_room_participant_room_member", columnNames = ["room_id", "member_id"])
    ],
    indexes = [
        Index(name = "idx_chat_room_participant_room", columnList = "room_id"),
        Index(name = "idx_chat_room_participant_member", columnList = "member_id"),
        Index(name = "idx_chat_room_participant_member_visible", columnList = "member_id, visible")
    ]
)
class ChatRoomParticipant(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    var room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now()

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    fun getId(): Long? = id

    fun leave(
        deletedAt: Instant
    ) {

        visible = false
        this.deletedAt = deletedAt
    }

    fun reactivate() {

        visible = true
        deletedAt = null
    }
}
