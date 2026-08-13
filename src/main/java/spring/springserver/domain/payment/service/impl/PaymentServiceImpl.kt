package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.exception.BlockchainCommitTimeoutException
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.data.response.PreparePaymentResponse
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.domain.payment.service.PaymentService
import spring.springserver.global.exception.exception.ApplicationException
import java.security.MessageDigest

@Service
class PaymentServiceImpl(
    private val tossPaymentsClient: TossPaymentsClient,
    private val keyService: KeyService,
    private val blockchainService: BlockchainService,
    private val estimateService: EstimateService,
    private val paymentRecordService: PaymentRecordService
): PaymentService {

    private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    override fun prepare(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long
    ): PreparePaymentResponse {

        return PreparePaymentResponse.of(
            paymentRecordService.create(
                preparePaymentRequest,
                memberId
            )
        )
    }

    /**
     * 승인 → 블록체인 기록 순서로 진행하고, 기록에 실패하면 승인을 보상 취소한다.
     * 외부 호출 사이사이의 상태 변경이 유실되지 않도록 트랜잭션은 PaymentRecordService가 개별로 관리한다.
     */
    override fun confirm(
        confirmPaymentRequest: ConfirmPaymentRequest,
        memberId: Long
    ): PaymentResponse {

        val stored = paymentRecordService.findByOrderId(confirmPaymentRequest.orderId)

        if (stored.getStatus() == PaymentStatus.CHAIN_RECORDED) {

            return tossPaymentsClient.findByOrderId(confirmPaymentRequest.orderId)
        }

        val payment = paymentRecordService.startConfirm(
            confirmPaymentRequest.orderId,
            confirmPaymentRequest.amount,
            memberId
        )

        val response = confirmOnToss(confirmPaymentRequest)
        val paymentKey = response.paymentKey
            ?: throw ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED)

        paymentRecordService.markDone(
            confirmPaymentRequest.orderId,
            paymentKey
        )

        val hash = sha256("$paymentKey | ${response.orderId} | ${response.totalAmount} | ${response.approvedAt}")
        val blockchainTxHash = try {

            recordOnChain(
                memberId,
                confirmPaymentRequest.orderId,
                payment.getContractUrl(),
                response,
                hash
            )
        } catch (exception: Exception) {

            log.error("블록체인 기록 실패. 결제를 보상 취소합니다. orderId={}", confirmPaymentRequest.orderId, exception)

            compensate(
                paymentKey,
                confirmPaymentRequest.orderId,
                exception.message
            )
        }

        paymentRecordService.markChainRecorded(
            confirmPaymentRequest.orderId,
            hash,
            blockchainTxHash
        )

        /**
         * 견적서 결제라면 체인 기록까지 끝난 뒤 결제 완료 처리한다.
         * 보상 취소된 결제가 완료로 남지 않도록 순서를 마지막에 둔다.
         */
        confirmPaymentRequest.estimateId?.let { estimateId ->

            estimateService.markAsPaid(
                estimateId,
                memberId
            )
        }

        return response
    }

    private fun confirmOnToss(
        confirmPaymentRequest: ConfirmPaymentRequest
    ): PaymentResponse {

        try {

            return tossPaymentsClient.confirm(confirmPaymentRequest)
        } catch (exception: Exception) {

            paymentRecordService.markAbandoned(
                confirmPaymentRequest.orderId,
                exception.message
            )

            throw exception
        }
    }

    private fun recordOnChain(
        memberId: Long,
        orderId: String,
        contractUrl: String,
        paymentResponse: PaymentResponse,
        hash: String
    ): String? {

        val paidAt = paymentResponse.approvedAt
            ?: throw ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED)
        val amount = paymentResponse.totalAmount
            ?: throw ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED)
        val buyerSignature = keyService.signHash(memberId, hash)
        val buyerAddress = keyService.deriveCosmosAddress(memberId)
        var lastException: Exception? = null

        repeat(CHAIN_RECORD_MAX_ATTEMPTS) { attempt ->

            try {

                return blockchainService.recordPayment(
                    buyerAddress,
                    orderId,
                    amount,
                    paidAt,
                    contractUrl,
                    hash,
                    buyerSignature
                )
            } catch (exception: BlockchainAlreadyRecordedException) {

                log.info("이미 체인에 기록된 주문입니다. 성공으로 처리합니다. orderId={}", orderId)

                return null
            } catch (exception: BlockchainCommitTimeoutException) {

                log.error("커밋 대기가 시간을 초과했습니다. 재시도하지 않고 보상 취소로 넘깁니다. orderId={}", orderId, exception)

                throw exception
            } catch (exception: Exception) {

                lastException = exception

                log.warn("블록체인 기록 시도 실패 ({}/{}). orderId={}", attempt + 1, CHAIN_RECORD_MAX_ATTEMPTS, orderId, exception)

                if (attempt < CHAIN_RECORD_MAX_ATTEMPTS - 1) Thread.sleep(CHAIN_RECORD_RETRY_DELAY_MILLIS)
            }
        }

        throw lastException
            ?: IllegalStateException("블록체인 기록에 실패했습니다.")
    }

    private fun compensate(
        paymentKey: String,
        orderId: String,
        failureReason: String?
    ): Nothing {

        try {

            tossPaymentsClient.cancel(
                paymentKey,
                CancelPaymentRequest(CHAIN_FAILURE_CANCEL_REASON),
                cancelIdempotencyKey(orderId)
            )
        } catch (exception: Exception) {

            log.error("보상 취소 실패. 스케줄러 재시도 대상으로 남깁니다. orderId={}, paymentKey={}", orderId, paymentKey, exception)

            paymentRecordService.markCancelPending(
                orderId,
                failureReason
            )

            throw ApplicationException(PaymentStatusCode.PAYMENT_CANCEL_FAILED)
        }

        paymentRecordService.markCanceledByChainFailure(
            orderId,
            failureReason
        )

        throw ApplicationException(PaymentStatusCode.PAYMENT_BLOCKCHAIN_RECORD_FAILED)
    }

    private fun sha256(
        input: String
    ): String {

        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    override fun findByPaymentKey(paymentKey: String): PaymentResponse {

        return tossPaymentsClient.findByPaymentKey(paymentKey)
    }

    override fun findByOrderId(orderId: String): PaymentResponse {

        return tossPaymentsClient.findByOrderId(orderId)
    }

    override fun cancel(
        paymentKey: String,
        cancelPaymentRequest: CancelPaymentRequest,
        idempotencyKey: String?
    ): PaymentResponse {

        return tossPaymentsClient.cancel(
            paymentKey,
            cancelPaymentRequest,
            idempotencyKey
        )
    }

    override fun issueVirtualAccount(virtualAccountRequest: VirtualAccountRequest): PaymentResponse {

        return tossPaymentsClient.issueVirtualAccount(virtualAccountRequest)
    }

    companion object {

        const val CHAIN_FAILURE_CANCEL_REASON = "블록체인 기록 실패로 인한 자동 취소"

        private const val CHAIN_RECORD_MAX_ATTEMPTS = 3
        private const val CHAIN_RECORD_RETRY_DELAY_MILLIS = 500L

        fun cancelIdempotencyKey(
            orderId: String
        ): String {

            return "chain-fail-$orderId"
        }
    }
}
