package spring.springserver.domain.call.support

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.exception.CallStatusCode
import spring.springserver.domain.call.repository.CallRepository
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.repository.ChatRoomParticipantRepository
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException

/**
 * 통화 서비스들이 공통으로 쓰는 조회와 권한 확인.
 *
 * 라이프사이클 서비스와 미디어 서비스가 같은 검증을 각자 들고 있으면
 * 한쪽만 고쳐져서 권한 구멍이 생기므로 한곳에 모아 둔다.
 */
@Component
class CallAccessSupport(
    private val callRepository: CallRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomParticipantRepository: ChatRoomParticipantRepository,
    private val chatService: ChatService,
    private val memberRepository: MemberRepository,
    private val profileService: ProfileService,
    private val jdbcTemplate: JdbcTemplate
) {

    fun getMemberByUsername(
        username: String
    ): Member =
        memberRepository.findByUsername(username)
            ?: throw ApplicationException(CallStatusCode.CALL_MEMBER_NOT_FOUND)

    fun getRoom(
        roomId: Long
    ): ChatRoom =
        chatRoomRepository.findByIdWithParticipants(roomId)
            ?: throw ApplicationException(CallStatusCode.CALL_ROOM_NOT_FOUND)

    /**
     * 통화를 찾고 요청자가 참여자인지 확인한다.
     * 참여자가 아니면 통화의 존재 여부도 알려주지 않도록 같은 경로로 막는다.
     */
    fun getParticipatingCall(
        callId: Long,
        memberId: Long?
    ): Call {

        val call = callRepository.findByIdWithParticipants(callId)
            ?: throw ApplicationException(CallStatusCode.CALL_NOT_FOUND)

        if (!call.isParticipant(memberId)) {

            throw ApplicationException(CallStatusCode.CALL_FORBIDDEN)
        }

        return call
    }

    /**
     * 상태를 바꾸는 요청용. 통화 행을 잠근 뒤 참여자인지 확인한다.
     * 조회 전용 트랜잭션에서는 쓰지 않는다(읽기 전용 트랜잭션에서 FOR UPDATE는 실패한다).
     */
    fun getParticipatingCallForUpdate(
        callId: Long,
        memberId: Long?
    ): Call {

        val call = callRepository.findByIdForUpdate(callId)
            ?: throw ApplicationException(CallStatusCode.CALL_NOT_FOUND)

        if (!call.isParticipant(memberId)) {

            throw ApplicationException(CallStatusCode.CALL_FORBIDDEN)
        }

        return call
    }

    /**
     * 새 통화를 만들기 전에 두 회원 단위로 트랜잭션 잠금을 잡는다.
     *
     * 진행 중 통화 확인 후 저장하는 사이에 A→B, B→A가 동시에 들어오면 둘 다 통과한다.
     * 아직 없는 행은 행 잠금으로 막을 수 없어 PostgreSQL advisory lock을 쓴다.
     * 교착을 피하려고 항상 회원 id 오름차순으로 잡고, 트랜잭션이 끝나면 자동으로 풀린다.
     */
    fun lockMembersForNewCall(
        memberIds: Collection<Long>
    ) {

        memberIds.distinct().sorted().forEach { memberId ->

            jdbcTemplate.queryForList(
                "select pg_advisory_xact_lock(?)",
                CALL_LOCK_NAMESPACE or memberId
            )
        }
    }

    /**
     * 참여 여부는 `chat_room_participant`가 단일 출처다.
     * 채팅 서비스를 거쳐 과거 방의 참여자 row 보정까지 함께 처리한다.
     */
    fun isRoomParticipant(
        room: ChatRoom,
        memberId: Long?
    ): Boolean {

        val roomId = room.getId()

        if (roomId == null || memberId == null) {

            return false
        }

        return chatService.isRoomParticipant(
            roomId = roomId,
            memberId = memberId
        )
    }

    fun getRoomCounterpart(
        room: ChatRoom,
        memberId: Long?
    ): Member =
        chatRoomParticipantRepository.findAllByRoomId(room.getId()!!)
            .firstOrNull { it.member.getId() != memberId }
            ?.member
            ?: throw ApplicationException(CallStatusCode.CALL_ROOM_NOT_FOUND)

    /**
     * 통화 응답에 실을 두 참여자의 프로필 이미지를 한 번의 조회로 가져온다.
     */
    fun getImageUrls(
        call: Call
    ): Map<Long, String?> =
        profileService.getImageUrlsByMemberIds(
            listOfNotNull(
                call.caller.getId(),
                call.callee.getId()
            )
        )

    companion object {

        /**
         * advisory lock 키의 상위 16비트. 다른 기능이 같은 회원 id로 잠금을 잡아도 겹치지 않게 구분한다.
         * 하위 48비트에 회원 id를 그대로 넣으므로 int로 자를 때처럼 다른 회원과 키가 겹치지 않는다.
         */
        private const val CALL_LOCK_NAMESPACE = 2301L shl 48
    }
}
