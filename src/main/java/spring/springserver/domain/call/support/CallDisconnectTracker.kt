package spring.springserver.domain.call.support

import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * STOMP 연결이 끊긴 회원을 기록해 두고, 유예 시간이 지나도 다시 붙지 않은 회원을 골라낸다.
 *
 * 끊기자마자 통화를 정리하면 새로고침 한 번에도 통화가 끊긴다. 그래서 유예 시간 동안 기다렸다가
 * 그때도 세션이 하나도 없을 때만 정리 대상으로 본다.
 *
 * 세션 정보(`SimpUserRegistry`)는 인스턴스마다 따로 있으므로 기록도 인스턴스 메모리에 둔다.
 * 여러 인스턴스로 늘리면서 다른 인스턴스로 재연결되면 연결된 회원도 정리될 수 있다.
 * 그때는 외부 브로커와 공유 세션 저장소가 필요하다.
 */
@Component
class CallDisconnectTracker(
    private val simpUserRegistry: SimpUserRegistry
) {

    private val disconnectedAt = ConcurrentHashMap<String, Instant>()

    fun markDisconnected(
        username: String,
        at: Instant
    ) {

        disconnectedAt[username] = at
    }

    /**
     * 유예 시간이 지난 기록을 꺼낸다. 그사이 다시 연결된 회원은 기록만 지우고 돌려주지 않는다.
     */
    fun drainExpired(
        now: Instant
    ): List<String> {

        val threshold = now.minus(GRACE_PERIOD)

        val expired = disconnectedAt.entries
            .filter { entry -> entry.value.isBefore(threshold) }
            .map { entry -> entry.key }

        expired.forEach { username -> disconnectedAt.remove(username) }

        return expired.filter { username -> simpUserRegistry.getUser(username) == null }
    }

    companion object {

        val GRACE_PERIOD: Duration = Duration.ofSeconds(15)
    }
}
