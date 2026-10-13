package spring.springserver.domain.call.event

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import spring.springserver.domain.call.support.CallDisconnectTracker
import java.time.Instant

/**
 * STOMP 세션이 끊기면 그 회원을 기록한다. 실제 통화 정리는 유예 시간이 지난 뒤 스케줄러가 한다.
 */
@Component
class CallSessionDisconnectListener(
    private val callDisconnectTracker: CallDisconnectTracker
) {

    @EventListener
    fun recordDisconnect(
        sessionDisconnectEvent: SessionDisconnectEvent
    ) {

        val username = sessionDisconnectEvent.user?.name
            ?: return

        callDisconnectTracker.markDisconnected(
            username,
            Instant.now()
        )
    }
}
