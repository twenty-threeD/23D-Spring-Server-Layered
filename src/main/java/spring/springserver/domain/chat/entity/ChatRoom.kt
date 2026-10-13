package spring.springserver.domain.chat.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

@Entity
@Table(
    name = "chat_room",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_chat_room_direct_key_post",
            columnNames = ["direct_chat_key", "post_id"]
        )
    ],
    indexes = [
        Index(name = "idx_chat_room_client", columnList = "client_id"),
        Index(name = "idx_chat_room_professional", columnList = "professional_id"),
        Index(name = "idx_chat_room_direct_key", columnList = "direct_chat_key"),
        Index(name = "idx_chat_room_post", columnList = "post_id")
    ]
)
class ChatRoom(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    var professional: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,
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

    @Column(name = "created_at", updatable = false)
    private var createdAt: Instant? = null

    /**
     * 컬럼이 생기기 전에 만들어진 방은 값이 null이므로 조회할 때 진행 중으로 본다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_status", length = 20)
    private var tradeStatus: TradeStatus? = TradeStatus.IN_PROGRESS

    @PrePersist
    fun prePersistDate() {

        createdAt = Instant.now()
    }

    fun getId(): Long? = id

    fun getCreatedAt(): Instant? = createdAt

    fun getTradeStatus(): TradeStatus = tradeStatus ?: TradeStatus.IN_PROGRESS

    fun completeTrade() {

        tradeStatus = TradeStatus.COMPLETED
    }

    fun cancelTrade() {

        tradeStatus = TradeStatus.CANCELED
    }

    fun restartTrade() {

        tradeStatus = TradeStatus.IN_PROGRESS
    }

    fun updateLastMessageMeta(
        at: Instant,
        preview: String
    ) {

        lastMessageAt = at
        lastMessagePreview = preview
    }

    companion object {

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
