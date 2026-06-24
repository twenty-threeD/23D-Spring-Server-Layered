package spring.springserver.domain.chat.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import spring.springserver.domain.member.entity.Member
import java.time.Instant

@Entity
@Table(
    name = "chat_message",
    indexes = [
        Index(name = "idx_chat_message_room_created_at", columnList = "chat_room_id, created_at"),
        Index(name = "idx_chat_message_sender", columnList = "sender_member_id")
    ]
)
class ChatMessage(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_member_id", nullable = false)
    var sender: Member,

    @Column(name = "message", nullable = false, length = 500)
    var message: String,

    @Enumerated(EnumType.STRING)
    var messageType: MessageType,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId(): Long? = id
}
