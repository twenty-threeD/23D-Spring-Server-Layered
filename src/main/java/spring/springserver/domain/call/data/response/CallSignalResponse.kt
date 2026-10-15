package spring.springserver.domain.call.data.response

import java.time.Instant

/**
 * STOMP `/user/queue/call`로 내려가는 통화 시그널.
 *
 * 토큰은 담지 않는다. 수신자는 INVITED를 받은 뒤 accept API를 호출해 자기 토큰을 받아간다.
 */
data class CallSignalResponse(
    val signal: CallSignalType,
    val call: CallResponse,
    /**
     * 이 시그널을 발생시킨 사용자. 타임아웃처럼 서버가 발생시킨 경우 null이다.
     */
    val fromUsername: String?,
    val occurredAt: Instant
) {

    companion object {

        fun of(
            signal: CallSignalType,
            call: CallResponse,
            fromUsername: String?,
            occurredAt: Instant
        ): CallSignalResponse =
            CallSignalResponse(
                signal = signal,
                call = call,
                fromUsername = fromUsername,
                occurredAt = occurredAt
            )
    }
}
