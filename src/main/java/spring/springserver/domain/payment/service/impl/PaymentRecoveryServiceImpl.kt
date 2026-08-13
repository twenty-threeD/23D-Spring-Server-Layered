package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.payment.repository.PaymentRepository
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.domain.payment.service.PaymentRecoveryService
import java.time.LocalDateTime

@Service
class PaymentRecoveryServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val paymentRecordService: PaymentRecordService,
    private val tossPaymentsClient: TossPaymentsClient
): PaymentRecoveryService {

    private val log = LoggerFactory.getLogger(PaymentRecoveryServiceImpl::class.java)

    override fun retryPendingCancels() {

        findRetryablePendingCancels().forEach { payment ->

            val paymentKey = payment.getPaymentKey()

            if (paymentKey == null) {

                log.error("CANCEL_PENDING인데 paymentKey가 없습니다. 수동 확인이 필요합니다. orderId={}", payment.getOrderId())

                paymentRecordService.markCancelFailed(payment.getOrderId())

                return@forEach
            }

            try {

                tossPaymentsClient.cancel(
                    paymentKey,
                    CancelPaymentRequest(PaymentServiceImpl.CHAIN_FAILURE_CANCEL_REASON),
                    PaymentServiceImpl.cancelIdempotencyKey(payment.getOrderId())
                )

                paymentRecordService.markCanceledByChainFailure(
                    payment.getOrderId(),
                    payment.getFailureReason()
                )

                log.info("보상 취소 재시도 성공. orderId={}", payment.getOrderId())
            } catch (exception: Exception) {

                log.warn("보상 취소 재시도 실패 ({}회차). orderId={}", payment.getCancelAttemptCount() + 1, payment.getOrderId(), exception)

                if (payment.getCancelAttemptCount() + 1 >= MAX_CANCEL_ATTEMPT_COUNT) {

                    log.error("보상 취소 재시도 한도 초과. 수동 확인이 필요합니다. orderId={}", payment.getOrderId())

                    paymentRecordService.markCancelFailed(payment.getOrderId())
                } else {

                    paymentRecordService.markCancelPending(
                        payment.getOrderId(),
                        payment.getFailureReason()
                    )
                }
            }
        }
    }

    override fun reconcileStuckPayments() {

        findStuckInProgress().forEach { payment ->

            try {

                val response = tossPaymentsClient.findByOrderId(payment.getOrderId())
                val paymentKey = response.paymentKey

                if (response.status == TOSS_STATUS_DONE && paymentKey != null) {

                    log.warn("중단된 승인 건이 토스에서는 승인 상태입니다. 취소 대상으로 전환합니다. orderId={}", payment.getOrderId())

                    paymentRecordService.markDone(
                        payment.getOrderId(),
                        paymentKey
                    )
                    paymentRecordService.markCancelPending(
                        payment.getOrderId(),
                        STUCK_FAILURE_REASON
                    )
                } else {

                    paymentRecordService.markAbandoned(
                        payment.getOrderId(),
                        STUCK_FAILURE_REASON
                    )
                }
            } catch (exception: Exception) {

                log.warn("중단 결제 정합화 실패. 다음 주기에 다시 시도합니다. orderId={}", payment.getOrderId(), exception)
            }
        }
    }

    private fun findRetryablePendingCancels(): List<Payment> {

        return paymentRepository.findRetryableByStatus(
            PaymentStatus.CANCEL_PENDING,
            MAX_CANCEL_ATTEMPT_COUNT
        )
    }

    private fun findStuckInProgress(): List<Payment> {

        return paymentRepository.findStuckByStatus(
            PaymentStatus.IN_PROGRESS,
            LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES)
        )
    }

    companion object {

        private const val MAX_CANCEL_ATTEMPT_COUNT = 5
        private const val STUCK_THRESHOLD_MINUTES = 10L
        private const val TOSS_STATUS_DONE = "DONE"
        private const val STUCK_FAILURE_REASON = "승인 처리 중 서버 중단으로 인한 정합화"
    }
}