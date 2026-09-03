package spring.springserver.domain.notification.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member
import java.time.Instant

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(name = "idx_notification_receiver_sent_at", columnList = "receiver_member_id, sent_at")
    ]
)
class Notification(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: NotificationType,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_member_id", nullable = false)
    var receiver: Member,

    @Column(name = "message", nullable = false, length = 1000)
    var message: String,

    @Column(name = "sent_at", nullable = false, updatable = false)
    var sentAt: Instant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_member_id")
    var sender: Member? = null,

    @Column(name = "chat_room_id")
    var roomId: Long? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId(): Long? = id
}