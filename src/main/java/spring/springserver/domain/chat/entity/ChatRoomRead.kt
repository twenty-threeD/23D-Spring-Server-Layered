package spring.springserver.domain.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import spring.springserver.domain.member.entity.Member
import java.time.Instant

@Entity
@Table(
    name = "chat_room_read",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_room_member",
            columnNames = ["chat_room_id", "member_id"]
        )
    ],
    indexes = [
        Index(name = "idx_room_read_room", columnList = "chat_room_id"),
        Index(name = "idx_room_read_member", columnList = "member_id")
    ]
)
class ChatRoomRead(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(name = "last_read_seq", nullable = false)
    var lastReadSeq: Long = 0L

    @Column(name = "updated_at")
    var updatedAt: Instant? = Instant.now()

    fun getId(): Long? = id

    fun markRead(
        seq: Long
    ) {

        if (seq > lastReadSeq) {

            lastReadSeq = seq
            updatedAt = Instant.now()
        }
    }
}
