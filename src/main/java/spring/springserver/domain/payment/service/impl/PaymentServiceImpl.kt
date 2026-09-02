package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.data.response.PaymentVerificationResponse
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.exception.BlockchainCommitTimeoutException
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.estimate.data.response.EstimateResponse
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
    private val paymentRecordService: PaymentRecordService,
    private val chatService: ChatService
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

        /**
         * 견적서 결제라면 승인을 요청하기 전에 금액부터 대조한다.
         * 조작된 금액으로 실제 결제가 일어나는 것을 막는다.
         * 결제 건을 선점하기 전에 확인해야 실패한 요청이 IN_PROGRESS로 남지 않는다.
         */
        confirmPaymentRequest.estimateId?.let { estimateId ->

            estimateService.validatePayable(
                estimateId,
                memberId,
                confirmPaymentRequest.amount
            )
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
         * 승인된 실제 금액으로 한 번 더 대조한 뒤 결제 완료 처리한다.
         * 체인 기록까지 끝난 뒤에 호출해야 보상 취소된 결제가 완료로 남지 않는다.
         * 이후 해당 견적서는 조회만 가능하다.
         */
        confirmPaymentRequest.estimateId?.let { estimateId ->
            notifyPaymentToChat(
                estimate = estimateService.markAsPaid(
                    estimateId,
                    memberId,
                    response.totalAmount ?: confirmPaymentRequest.amount
                ),
                memberId = memberId,
                paymentResponse = response,
                paymentHash = hash,
                blockchainTxHash = blockchainTxHash
            )
        }

        return response
    }

    override fun verify(
        orderId: String,
        memberId: Long
    ): PaymentVerificationResponse {

        val payment = paymentRecordService.findByOrderId(orderId = orderId)

        if (payment.getMemberId() != memberId) throw ApplicationException(PaymentStatusCode.PAYMENT_MEMBER_MISMATCH)

        val record = blockchainService.findRecord(orderId = orderId)
            ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_RECORDED_ON_CHAIN)
        val response = tossPaymentsClient.findByOrderId(orderId = orderId)
        val recalculateHash = sha256(
            "${response.paymentKey} | ${response.orderId} | ${response.totalAmount} | ${response.approvedAt}"
        )

        return PaymentVerificationResponse.of(
            record,
            recalculatedHash = recalculateHash,
            keyService.verifySignature(
                payment.getMemberId(),
                record.paymentHash,
                record.buyerSignature
            ),
            record.buyerAddress == keyService.deriveCosmosAddress(payment.getMemberId()),
            record.amount == response.totalAmount
        )
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
            } catch (_: BlockchainAlreadyRecordedException) {

                return null
            } catch (exception: BlockchainCommitTimeoutException) {

                throw exception
            } catch (exception: Exception) {

                lastException = exception

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
        } catch (_: Exception) {

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

    private fun notifyPaymentToChat(
        estimate: EstimateResponse,
        memberId: Long,
        paymentResponse: PaymentResponse,
        paymentHash: String,
        blockchainTxHash: String?
    ) {

        try {

            chatService.sendPaymentMessage(
                roomId = chatService.findDirectRoomId(
                    clientId = estimate.clientId ?: return,
                    professionalId = estimate.professionalId ?: return,
                    postId = estimate.postId ?: return
                ) ?: return,
                senderMemberId = memberId,
                payment = ChatPaymentResponse.of(
                    orderId = paymentResponse.orderId ?: return,
                    orderName = paymentResponse.orderName ?: DEFAULT_ORDER_NAME,
                    amount = paymentResponse.totalAmount ?: estimate.totalPay,
                    paymentHash = paymentHash,
                    blockchainTxHash = blockchainTxHash
                )
            )
        } catch (exception: Exception) {

            log.error("결제 채팅 메시지 전송 실패. orderId={}", paymentResponse.orderId, exception)
        }
    }

    companion object {

        const val CHAIN_FAILURE_CANCEL_REASON = "블록체인 기록 실패로 인한 자동 취소"
        const val DEFAULT_ORDER_NAME = "결제"

        private const val CHAIN_RECORD_MAX_ATTEMPTS = 3
        private const val CHAIN_RECORD_RETRY_DELAY_MILLIS = 500L

        fun cancelIdempotencyKey(
            orderId: String
        ): String {

            return "chain-fail-$orderId"
        }
    }
}
