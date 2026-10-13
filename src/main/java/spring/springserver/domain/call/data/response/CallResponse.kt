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

        /**
         * [imageUrls]는 회원 id → 프로필 이미지. 두 참여자를 한 번에 조회해 넘긴다.
         */
        fun of(
            call: Call,
            imageUrls: Map<Long, String?>
        ): CallResponse =
            CallResponse(
                callId = call.getId(),
                roomId = call.room.getId(),
                callType = call.callType,
                status = call.status,
                caller = CallParticipantResponse.of(
                    call = call,
                    member = call.caller,
                    imageUrl = call.caller.getId()?.let { imageUrls[it] }
                ),
                callee = CallParticipantResponse.of(
                    call = call,
                    member = call.callee,
                    imageUrl = call.callee.getId()?.let { imageUrls[it] }
                ),
                createdAt = call.getCreatedAt(),
                startedAt = call.startedAt,
                endedAt = call.endedAt,
                durationSeconds = call.getDurationSeconds()
            )
    }
}
