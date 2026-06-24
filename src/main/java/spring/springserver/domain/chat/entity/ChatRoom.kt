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
import kotlin.math.max
import kotlin.math.min

@Entity
@Table(
    name = "chat_room",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_chat_room_direct_key", columnNames = ["direct_chat_key"])
    ],
    indexes = [
        Index(name = "idx_chat_room_client", columnList = "client_id"),
        Index(name = "idx_chat_room_professional", columnList = "professionl_id"),
        Index(name = "idx_chat_room_direct_key", columnList = "direct_chat_key")
    ]
)
class ChatRoom(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professionl_id", nullable = false)
    var professional: Member,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Column(name = "direct_chat_key", length = 100, updatable = false)
    var directChatKey: String? = generateDirectChatKey(client.getId()!!, professional.getId()!!)

    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null

    @Column(name = "last_message_preview", length = 200)
    var lastMessagePreview: String? = null

    fun getId(): Long? = id

    fun updateLastMessageMeta(
        at: Instant,
        preview: String
    ) {

        lastMessageAt = at
        lastMessagePreview = preview
    }

    companion object {

        @JvmStatic
        fun generateDirectChatKey(
            memberId1: Long,
            memberId2: Long
        ): String {

            val first = min(memberId1, memberId2)
            val second = max(memberId1, memberId2)

            return "$first:$second"
        }
    }
}
