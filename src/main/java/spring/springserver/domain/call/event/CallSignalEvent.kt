package spring.springserver.domain.call.event

import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSignalType
import java.time.Instant

/**
 * 통화 상태가 바뀌었음을 알리는 이벤트.
 *
 * 엔티티가 아니라 트랜잭션 안에서 뜬 [CallResponse] 스냅샷을 담는다.
 * 실제 전송은 커밋 이후에 일어나므로, 엔티티를 담으면 그 시점에 지연 로딩이 터진다.
 */
data class CallSignalEvent(
    val signal: CallSignalType,
    val call: CallResponse,
    val fromUsername: String?,
    val occurredAt: Instant
) {

    companion object {

        fun of(
            signal: CallSignalType,
            call: CallResponse,
            fromUsername: String?
        ): CallSignalEvent =
            CallSignalEvent(
                signal = signal,
                call = call,
                fromUsername = fromUsername,
                occurredAt = Instant.now()
            )
    }
}
