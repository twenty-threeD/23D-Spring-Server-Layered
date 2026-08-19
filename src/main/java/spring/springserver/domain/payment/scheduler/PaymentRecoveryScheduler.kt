package spring.springserver.domain.payment.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import spring.springserver.domain.payment.service.PaymentRecoveryService

@Component
class PaymentRecoveryScheduler(
    private val paymentRecoveryService: PaymentRecoveryService
) {

    /**
     * 5분마다 보상 취소에 실패한 결제를 다시 취소한다.
     */
    @Scheduled(cron = "0 */5 * * * *")
    fun retryPendingCancels() {

        paymentRecoveryService.retryPendingCancels()
    }

    /**
     * 10분마다 승인 도중 중단된 결제를 토스 조회 결과로 정합화한다.
     */
    @Scheduled(cron = "0 */10 * * * *")
    fun reconcileStuckPayments() {

        paymentRecoveryService.reconcileStuckPayments()
    }
}