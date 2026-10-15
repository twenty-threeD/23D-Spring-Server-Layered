package spring.springserver.domain.payment.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import spring.springserver.domain.payment.service.PaymentRecoveryService
import spring.springserver.global.util.RedisLockExecutor
import java.time.Duration

/**
 * 인스턴스가 여러 대여도 작업마다 한 대만 돌도록 잠금을 잡는다.
 * 잠금이 없으면 같은 결제를 동시에 처리해 취소 재시도 횟수가 인스턴스 수만큼 늘어난다.
 * 작업끼리는 다루는 결제 상태가 달라 잠금을 따로 둔다.
 */
@Component
class PaymentRecoveryScheduler(
    private val paymentRecoveryService: PaymentRecoveryService,
    private val redisLockExecutor: RedisLockExecutor
) {

    /**
     * 5분마다 보상 취소에 실패한 결제를 다시 취소한다.
     */
    @Scheduled(cron = "0 */5 * * * *")
    fun retryPendingCancels() {

        redisLockExecutor.runWithLock(
            key = RETRY_PENDING_CANCELS_LOCK_KEY,
            ttl = LOCK_TTL
        ) {

            paymentRecoveryService.retryPendingCancels()
        }
    }

    /**
     * 10분마다 승인 도중 중단된 결제를 토스 조회 결과로 정합화한다.
     */
    @Scheduled(cron = "0 */10 * * * *")
    fun reconcileStuckPayments() {

        redisLockExecutor.runWithLock(
            key = RECONCILE_STUCK_PAYMENTS_LOCK_KEY,
            ttl = LOCK_TTL
        ) {

            paymentRecoveryService.reconcileStuckPayments()
        }
    }

    /**
     * 5분마다 토스 취소 후 상태 반영에 실패한 결제를 토스 조회 결과로 정합화한다.
     */
    @Scheduled(cron = "30 */5 * * * *")
    fun reconcileUnsyncedCancels() {

        redisLockExecutor.runWithLock(
            key = RECONCILE_UNSYNCED_CANCELS_LOCK_KEY,
            ttl = LOCK_TTL
        ) {

            paymentRecoveryService.reconcileUnsyncedCancels()
        }
    }

    /**
     * 3분마다 체인 커밋 결과가 미확정인 결제를 원장 조회로 확정한다.
     */
    @Scheduled(cron = "0 */3 * * * *")
    fun reconcileUnconfirmedChainRecords() {

        redisLockExecutor.runWithLock(
            key = RECONCILE_UNCONFIRMED_CHAIN_RECORDS_LOCK_KEY,
            ttl = LOCK_TTL
        ) {

            paymentRecoveryService.reconcileUnconfirmedChainRecords()
        }
    }

    companion object {

        private const val RETRY_PENDING_CANCELS_LOCK_KEY = "payment:recovery:retry-pending-cancels:lock"

        private const val RECONCILE_STUCK_PAYMENTS_LOCK_KEY = "payment:recovery:reconcile-stuck-payments:lock"

        private const val RECONCILE_UNSYNCED_CANCELS_LOCK_KEY = "payment:recovery:reconcile-unsynced-cancels:lock"

        private const val RECONCILE_UNCONFIRMED_CHAIN_RECORDS_LOCK_KEY = "payment:recovery:reconcile-unconfirmed-chain-records:lock"

        /**
         * 가장 짧은 주기(3분)보다 짧게 잡아, 실행 중 인스턴스가 죽어도 다음 주기는 돌게 한다.
         */
        private val LOCK_TTL: Duration = Duration.ofMinutes(2)
    }
}