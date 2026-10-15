package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.service.ChatService
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
    private val tossPaymentsClient: TossPaymentsClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val chatService: ChatService,
    private val blockchainService: BlockchainService
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

    /**
     * 토스 취소는 끝났는데 DB 반영이 실패한 결제를 토스 상태 기준으로 다시 반영한다.
     * 반영에 성공한 건만 대상에서 빼고, 실패하면 다음 주기에 다시 시도한다.
     */
    override fun reconcileUnsyncedCancels() {

        val paymentKeys = redisTemplate.opsForSet().members(PaymentServiceImpl.UNSYNCED_CANCEL_KEY)
            ?: return

        paymentKeys.forEach { paymentKey ->

            try {

                val response = tossPaymentsClient.findByPaymentKey(paymentKey)
                val payment = paymentRecordService.markCanceled(
                    paymentKey,
                    response.status,
                    response.balanceAmount
                )

                /**
                 * markCanceled는 토스 상태가 취소가 아니면 아무것도 바꾸지 않고 반환한다.
                 * 실제로 취소 상태가 반영된 건만 대상에서 빼야 다음 주기에 다시 시도할 수 있다.
                 */
                if (payment.getStatus() != PaymentStatus.CANCELED && payment.getStatus() != PaymentStatus.PARTIAL_CANCELED) {

                    log.warn("토스 상태가 취소가 아니어서 반영하지 못했습니다. 다음 주기에 다시 시도합니다. paymentKey={}, tossStatus={}", paymentKey, response.status)

                    return@forEach
                }

                /**
                 * 전액 취소면 채팅방 거래도 취소로 맞춘다. 실패하면 대상에 남겨 다음 주기에 다시 시도한다.
                 */
                if (payment.getStatus() == PaymentStatus.CANCELED) {

                    payment.getRoomId()?.let { roomId -> chatService.cancelTrade(roomId) }
                }

                redisTemplate.opsForSet().remove(
                    PaymentServiceImpl.UNSYNCED_CANCEL_KEY,
                    paymentKey
                )

                log.info("취소 상태 정합화 성공. paymentKey={}, tossStatus={}", paymentKey, response.status)
            } catch (exception: Exception) {

                log.warn("취소 상태 정합화 실패. 다음 주기에 다시 시도합니다. paymentKey={}", paymentKey, exception)
            }
        }
    }

    /**
     * 커밋 결과를 확정하지 못한 결제는 뒤늦게 원장에 올라올 수 있다.
     * 원장에 기록이 보이면 완료로 수렴시키고, 유예 한도까지 보이지 않으면 그때 보상 취소로 넘긴다.
     */
    override fun reconcileUnconfirmedChainRecords() {

        paymentRepository.findAllByStatus(PaymentStatus.CHAIN_UNCONFIRMED).forEach { payment ->

            val orderId = payment.getOrderId()
            val paymentHash = payment.getPaymentHash()

            /**
             * 해시가 없으면 완료로도 돌릴 수 없는 건이라 사람이 봐야 한다. 취소 쪽으로 넘긴다.
             */
            if (paymentHash == null) {

                log.error("CHAIN_UNCONFIRMED인데 결제 해시가 없습니다. 보상 취소 대상으로 넘깁니다. orderId={}", orderId)

                paymentRecordService.markCancelPending(
                    orderId,
                    CHAIN_UNCONFIRMED_EXPIRED_REASON
                )

                return@forEach
            }

            try {

                val record = blockchainService.findRecord(orderId = orderId)

                if (record != null) {

                    completeLateChainRecord(
                        payment,
                        record.paymentHash
                    )

                    log.info("뒤늦게 원장에 기록된 결제를 완료로 반영했습니다. orderId={}", orderId)

                    return@forEach
                }

                if (payment.getChainCheckAttemptCount() >= MAX_CHAIN_CHECK_ATTEMPT_COUNT) {

                    log.warn("유예 한도까지 원장 기록이 없어 보상 취소 대상으로 넘깁니다. orderId={}", orderId)

                    paymentRecordService.markCancelPending(
                        orderId,
                        CHAIN_UNCONFIRMED_EXPIRED_REASON
                    )

                    return@forEach
                }

                paymentRecordService.markChainUnconfirmed(
                    orderId,
                    paymentHash,
                    payment.getBlockchainTxHash(),
                    payment.getFailureReason()
                )
            } catch (exception: Exception) {

                log.warn("체인 기록 확인에 실패했습니다. 다음 주기에 다시 시도합니다. orderId={}", orderId, exception)
            }
        }
    }

    /**
     * 뒤늦게 확인된 체인 기록을 완료로 반영한다.
     * 거래 상태와 채팅 메시지는 결제 자체를 되돌릴 이유가 아니므로 실패해도 기록만 남긴다.
     */
    private fun completeLateChainRecord(
        payment: Payment,
        paymentHash: String
    ) {

        paymentRecordService.markChainRecorded(
            payment.getOrderId(),
            paymentHash,
            payment.getBlockchainTxHash()
        )

        val roomId = payment.getRoomId()
            ?: return

        try {

            chatService.completeTrade(roomId)
            chatService.sendPaymentMessage(
                roomId,
                payment.getMemberId(),
                ChatPaymentResponse.of(
                    payment.getOrderId(),
                    payment.getOrderName().orEmpty(),
                    payment.getAmount(),
                    paymentHash,
                    payment.getBlockchainTxHash()
                )
            )
        } catch (exception: Exception) {

            log.warn("뒤늦은 체인 기록 반영 후 거래 상태를 갱신하지 못했습니다. orderId={}, roomId={}", payment.getOrderId(), roomId, exception)
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
        private const val MAX_CHAIN_CHECK_ATTEMPT_COUNT = 5
        private const val CHAIN_UNCONFIRMED_EXPIRED_REASON = "블록체인 기록 미확인 유예 초과로 인한 자동 취소"
        private const val STUCK_FAILURE_REASON = "승인 처리 중 서버 중단으로 인한 정합화"
    }
}