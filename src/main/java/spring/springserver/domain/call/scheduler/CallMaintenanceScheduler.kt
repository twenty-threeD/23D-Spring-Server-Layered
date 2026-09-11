package spring.springserver.domain.call.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import spring.springserver.domain.call.service.CallMaintenanceService
import spring.springserver.global.util.RedisLockExecutor
import java.time.Duration

@Component
class CallMaintenanceScheduler(
    private val callMaintenanceService: CallMaintenanceService,
    private val redisLockExecutor: RedisLockExecutor
) {

    /**
     * 10초마다 방치된 통화를 정리한다.
     * 벨이 계속 울리는 상태로 남지 않도록 짧은 주기로 돈다.
     *
     * 인스턴스가 여러 대여도 한 대만 돌도록 잠금을 잡는다.
     * 잠금이 없으면 같은 통화의 MISSED 시그널이 인스턴스 수만큼 중복 발송된다.
     */
    @Scheduled(fixedDelay = 10_000)
    fun expireStaleCalls() {

        redisLockExecutor.runWithLock(
            key = LOCK_KEY,
            ttl = LOCK_TTL
        ) {

            callMaintenanceService.expireStaleCalls()
        }
    }

    companion object {

        private const val LOCK_KEY = "call:maintenance:lock"
        private val LOCK_TTL: Duration = Duration.ofSeconds(30)
    }
}
