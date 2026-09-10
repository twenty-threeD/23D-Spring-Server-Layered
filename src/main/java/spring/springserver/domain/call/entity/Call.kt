package spring.springserver.domain.call.entity

import jakarta.persistence.*
import spring.springserver.domain.call.exception.CallStatusCode
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.member.entity.Member
import spring.springserver.global.exception.exception.ApplicationException
import java.time.Duration
import java.time.Instant

/**
 * Agora RTC 채널 하나에 대응하는 1:1 통화 세션.
 *
 * uid는 Agora 채널 안에서만 유일하면 되므로 통화를 만들 때 채널 단위로 네 개를 미리 뽑아둔다.
 * 화면 공유는 Agora에서 별도 uid로 채널에 한 번 더 접속하는 방식이라 카메라 uid와 따로 관리한다.
 */
@Entity
@Table(
    name = "call_session",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_call_session_channel_name",
            columnNames = ["channel_name"]
        )
    ],
    indexes = [
        Index(name = "idx_call_session_room", columnList = "room_id"),
        Index(name = "idx_call_session_caller", columnList = "caller_id"),
        Index(name = "idx_call_session_callee", columnList = "callee_id"),
        Index(name = "idx_call_session_status", columnList = "status")
    ]
)
class Call(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    var room: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caller_id", nullable = false)
    var caller: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "callee_id", nullable = false)
    var callee: Member,

    @Column(name = "channel_name", nullable = false, length = 64, updatable = false)
    var channelName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 20, updatable = false)
    var callType: CallType,

    @Column(name = "caller_uid", nullable = false, updatable = false)
    var callerUid: Int,

    @Column(name = "callee_uid", nullable = false, updatable = false)
    var calleeUid: Int,

    @Column(name = "caller_screen_uid", nullable = false, updatable = false)
    var callerScreenUid: Int,

    @Column(name = "callee_screen_uid", nullable = false, updatable = false)
    var calleeScreenUid: Int,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CallStatus = CallStatus.RINGING

    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "audioEnabled",
            column = Column(name = "caller_audio_enabled", nullable = false)
        ),
        AttributeOverride(
            name = "videoEnabled",
            column = Column(name = "caller_video_enabled", nullable = false)
        )
    )
    var callerMedia: CallMediaState = CallMediaState.of(callType)

    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "audioEnabled",
            column = Column(name = "callee_audio_enabled", nullable = false)
        ),
        AttributeOverride(
            name = "videoEnabled",
            column = Column(name = "callee_video_enabled", nullable = false)
        )
    )
    var calleeMedia: CallMediaState = CallMediaState.of(callType)

    /**
     * 현재 화면을 공유 중인 참여자. 공유 중이 아니면 null이다.
     */
    @Column(name = "screen_sharing_member_id")
    var screenSharingMemberId: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    private var createdAt: Instant = Instant.now()

    @Column(name = "started_at")
    var startedAt: Instant? = null

    @Column(name = "ended_at")
    var endedAt: Instant? = null

    @Column(name = "ended_by_member_id")
    var endedByMemberId: Long? = null

    fun getId(): Long? = id

    fun getCreatedAt(): Instant = createdAt

    fun accept(
        at: Instant
    ) {

        requireStatus(CallStatus.RINGING)

        status = CallStatus.ACCEPTED
        startedAt = at
    }

    fun reject(
        at: Instant,
        memberId: Long?
    ) {

        requireStatus(CallStatus.RINGING)

        finish(
            status = CallStatus.REJECTED,
            at = at,
            memberId = memberId
        )
    }

    fun cancel(
        at: Instant,
        memberId: Long?
    ) {

        requireStatus(CallStatus.RINGING)

        finish(
            status = CallStatus.CANCELED,
            at = at,
            memberId = memberId
        )
    }

    fun miss(
        at: Instant
    ) {

        requireStatus(CallStatus.RINGING)

        finish(
            status = CallStatus.MISSED,
            at = at,
            memberId = null
        )
    }

    fun end(
        at: Instant,
        memberId: Long?
    ) {

        requireStatus(CallStatus.ACCEPTED)

        finish(
            status = CallStatus.ENDED,
            at = at,
            memberId = memberId
        )
    }

    /**
     * 마이크/카메라 송출 상태를 갱신한다. 연결된 통화에서만 의미가 있다.
     */
    fun updateMedia(
        memberId: Long?,
        audioEnabled: Boolean,
        videoEnabled: Boolean
    ) {

        requireStatus(CallStatus.ACCEPTED)

        mediaOf(memberId).update(
            audioEnabled = audioEnabled,
            videoEnabled = videoEnabled
        )
    }

    fun startScreenShare(
        memberId: Long
    ) {

        requireStatus(CallStatus.ACCEPTED)

        val sharingMemberId = screenSharingMemberId

        if (sharingMemberId != null && sharingMemberId != memberId) {

            throw ApplicationException(CallStatusCode.SCREEN_SHARE_OCCUPIED)
        }

        screenSharingMemberId = memberId
    }

    fun stopScreenShare(
        memberId: Long
    ) {

        if (screenSharingMemberId != memberId) {

            throw ApplicationException(CallStatusCode.SCREEN_SHARE_NOT_STARTED)
        }

        screenSharingMemberId = null
    }

    fun isCaller(
        memberId: Long?
    ): Boolean = caller.getId() == memberId

    fun isParticipant(
        memberId: Long?
    ): Boolean = isCaller(memberId) || callee.getId() == memberId

    fun isScreenSharing(
        memberId: Long?
    ): Boolean = memberId != null && screenSharingMemberId == memberId

    /**
     * 카메라(또는 마이크) 스트림용 uid.
     */
    fun cameraUidOf(
        memberId: Long?
    ): Int = if (isCaller(memberId)) callerUid else calleeUid

    /**
     * 화면 공유용 uid. 카메라 uid와 동시에 같은 채널에 접속한다.
     */
    fun screenUidOf(
        memberId: Long?
    ): Int = if (isCaller(memberId)) callerScreenUid else calleeScreenUid

    fun mediaOf(
        memberId: Long?
    ): CallMediaState = if (isCaller(memberId)) callerMedia else calleeMedia

    fun counterpartOf(
        memberId: Long?
    ): Member = if (isCaller(memberId)) callee else caller

    fun participantOf(
        memberId: Long?
    ): Member = if (isCaller(memberId)) caller else callee

    /**
     * 연결된 뒤 종료될 때까지의 통화 시간(초). 연결되지 않았으면 null이다.
     */
    fun getDurationSeconds(): Long? {

        val startedAt = startedAt
        val endedAt = endedAt

        if (startedAt == null || endedAt == null) {

            return null
        }

        return Duration.between(startedAt, endedAt).seconds
    }

    private fun finish(
        status: CallStatus,
        at: Instant,
        memberId: Long?
    ) {

        this.status = status
        this.endedAt = at
        this.endedByMemberId = memberId
        this.screenSharingMemberId = null
    }

    private fun requireStatus(
        expected: CallStatus
    ) {

        if (status != expected) {

            throw ApplicationException(CallStatusCode.CALL_INVALID_STATUS)
        }
    }
}