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

    @Column(name = "message", nullable = false, length = 1000)
    var message: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,

    @Column(name = "attachment_urls", length = 2000)
    var attachmentUrlsText: String? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId(): Long? = id

    fun attachmentUrls(): List<String> =
        attachmentUrlsText
            ?.split(ATTACHMENT_DELIMITER)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    companion object {

        private const val ATTACHMENT_DELIMITER = "|"

        fun joinAttachmentUrls(attachmentUrls: List<String>): String? =
            attachmentUrls
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(ATTACHMENT_DELIMITER)
    }
}
