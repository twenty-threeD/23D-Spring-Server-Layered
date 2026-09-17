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
import spring.springserver.domain.chat.data.response.ChatMessageType
import spring.springserver.domain.member.entity.Member
import java.time.Instant

/**
 * 채팅 메시지의 진실 원본.
 *
 * Redis 리스트는 최근 3일치 캐시일 뿐이고, 그보다 오래된 메시지와 캐시 미스는 이 테이블에서 읽는다.
 * 커서 페이지네이션이 `id < :lastId ORDER BY id DESC`로 동작해야 하므로 메시지 식별자는 이 PK가 발급한다.
 */
@Entity
@Table(
    name = "chat_message",
    indexes = [
        Index(name = "idx_chat_message_room_id_desc", columnList = "chat_room_id, id DESC"),
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

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    var messageType: ChatMessageType = ChatMessageType.TEXT,

    @Column(name = "attachment_urls", length = 2000)
    var attachmentUrlsText: String? = null,

    /**
     * 결제 메시지의 상세 페이로드(JSON). TEXT 메시지에서는 null이다.
     * Redis 캐시가 만료된 뒤에도 결제 메시지를 그대로 복원하려고 저장한다.
     */
    @Column(name = "payment_payload", length = 1000)
    var paymentPayload: String? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId() = id

    fun attachmentUrls(): List<String> =
        attachmentUrlsText
            ?.split(ATTACHMENT_DELIMITER)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    companion object {

        private const val ATTACHMENT_DELIMITER = "|"

        fun joinAttachmentUrls(
            attachmentUrls: List<String>
        ): String? =
            attachmentUrls
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(ATTACHMENT_DELIMITER)
    }
}
