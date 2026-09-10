package spring.springserver.domain.call.data.response

import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import spring.springserver.domain.call.entity.CallType
import java.time.Instant

/**
 * 통화의 현재 상태 전체. 토큰은 담지 않는다.
 *
 * 커밋 이후에 STOMP로 내보낼 때도 이 스냅샷을 그대로 쓴다.
 * 엔티티를 트랜잭션 밖으로 들고 나가면 지연 로딩이 터지기 때문이다.
 */
data class CallResponse(
    val callId: Long?,
    val roomId: Long?,
    val callType: CallType,
    val status: CallStatus,
    val caller: CallParticipantResponse,
    val callee: CallParticipantResponse,
    val createdAt: Instant,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?
) {

    companion object {

        fun of(
            call: Call
        ): CallResponse =
            CallResponse(
                callId = call.getId(),
                roomId = call.room.getId(),
                callType = call.callType,
                status = call.status,
                caller = CallParticipantResponse.of(
                    call = call,
                    member = call.caller
                ),
                callee = CallParticipantResponse.of(
                    call = call,
                    member = call.callee
                ),
                createdAt = call.getCreatedAt(),
                startedAt = call.startedAt,
                endedAt = call.endedAt,
                durationSeconds = call.getDurationSeconds()
            )
    }
}
