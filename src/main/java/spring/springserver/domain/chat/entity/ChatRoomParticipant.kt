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

    /**
     * 마지막으로 채팅방을 나간 시각.
     *
     * 단순한 삭제 이력이 아니라 "이 시각 이전 메시지는 이 참여자에게 보이지 않는다"는 워터마크로 쓴다.
     * 다시 입장해도(`rejoin`) 이 값을 지우지 않는다. 지우면 나가기 전 메시지가 전부 다시 보인다.
     */
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    fun getId(): Long? = id

    fun leave(
        deletedAt: Instant
    ) {

        visible = false
        this.deletedAt = deletedAt
    }

    /**
     * 채팅방을 목록에 다시 노출시킨다.
     *
     * `deletedAt` 워터마크는 의도적으로 유지한다. 나가기 전 메시지는 계속 가려진 상태로 둔다.
     */
    fun reactivate() {

        visible = true
    }
}
