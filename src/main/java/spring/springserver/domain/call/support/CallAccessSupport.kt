package spring.springserver.domain.call.support

import org.springframework.stereotype.Component
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.exception.CallStatusCode
import spring.springserver.domain.call.repository.CallRepository
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
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
    private val memberRepository: MemberRepository
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

    fun isRoomParticipant(
        room: ChatRoom,
        memberId: Long?
    ): Boolean =
        room.client.getId() == memberId || room.professional.getId() == memberId

    fun getRoomCounterpart(
        room: ChatRoom,
        memberId: Long?
    ): Member =
        if (room.client.getId() == memberId) room.professional else room.client
}
