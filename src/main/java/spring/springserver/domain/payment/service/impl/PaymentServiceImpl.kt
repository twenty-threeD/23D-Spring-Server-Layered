package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.data.response.PaymentVerificationResponse
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.exception.BlockchainCommitTimeoutException
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.ConfirmPaymentResponse
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.data.response.PreparePaymentResponse
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.domain.payment.service.PaymentService
import spring.springserver.global.config.blockchain.CosmosProperties
import spring.springserver.global.exception.exception.ApplicationException
import java.security.MessageDigest

@Service
class PaymentServiceImpl(
    private val tossPaymentsClient: TossPaymentsClient,
    private val keyService: KeyService,
    private val blockchainService: BlockchainService,
    private val estimateService: EstimateService,
    private val paymentRecordService: PaymentRecordService,
    private val chatService: ChatService,
    private val cosmosProperties: CosmosProperties
): PaymentService {

    private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    override fun prepare(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long
    ): PreparePaymentResponse {

        /**
         * 방 번호는 요청 본문으로 들어오므로 그대로 믿으면 남의 채팅방에 결제 메시지를 밀어 넣을 수 있다.
         * 결제를 만들기 전에 요청자가 그 방의 당사자인지 확인한다.
         */
        preparePaymentRequest.roomId?.let { roomId ->

            val participant = chatService.isRoomParticipant(
                roomId,
                memberId
            )

            if (!participant) {

                throw ApplicationException(PaymentStatusCode.PAYMENT_CHAT_ROOM_FORBIDDEN)
            }
        }

        return PreparePaymentResponse.of(
            paymentRecordService.create(
                preparePaymentRequest,
                memberId
            )
        )
    }

    override fun confirm(
        confirmPaymentRequest: ConfirmPaymentRequest,
        memberId: Long
    ): ConfirmPaymentResponse {

        val stored = paymentRecordService.findByOrderId(confirmPaymentRequest.orderId)

        if (stored.getStatus() == PaymentStatus.CHAIN_RECORDED) {

            return ConfirmPaymentResponse.of(
                payment = tossPaymentsClient.findByOrderId(confirmPaymentRequest.orderId),
                paymentHash = stored.getPaymentHash()
                    ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_RECORDED_ON_CHAIN),
                blockchainTxHash = stored.getBlockchainTxHash()
            )
        }

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

        /**
         * 이미 체인에 기록된 주문이면 새 트랜잭션이 없으므로, 앞선 시도에서 확보해 둔 해시를 그대로 쓴다.
         */
        val resolvedTxHash = blockchainTxHash ?: payment.getBlockchainTxHash()

        paymentRecordService.markChainRecorded(
            confirmPaymentRequest.orderId,
            hash,
            resolvedTxHash
        )

        confirmPaymentRequest.estimateId?.let { estimateId ->

            estimateService.markAsPaid(
                estimateId,
                memberId,
                response.totalAmount ?: confirmPaymentRequest.amount
            )
        }

        sendPaymentMessage(
            payment,
            response,
            memberId,
            hash,
            resolvedTxHash
        )

        return ConfirmPaymentResponse.of(
            payment = response,
            paymentHash = hash,
            blockchainTxHash = resolvedTxHash
        )
    }

    /**
     * 결제가 시작된 채팅방에 결제 완료 메시지를 남긴다.
     * 결제와 체인 기록은 이미 끝난 뒤이므로, 메시지 전송이 실패해도 결제를 되돌리지 않고 기록만 남긴다.
     */
    private fun sendPaymentMessage(
        payment: Payment,
        paymentResponse: PaymentResponse,
        memberId: Long,
        paymentHash: String,
        blockchainTxHash: String?
    ) {

        val roomId = payment.getRoomId()
            ?: return

        try {

            chatService.sendPaymentMessage(
                roomId,
                memberId,
                ChatPaymentResponse.of(
                    payment.getOrderId(),
                    payment.getOrderName().orEmpty(),
                    paymentResponse.totalAmount ?: payment.getAmount(),
                    paymentHash,
                    blockchainTxHash
                )
            )
        } catch (exception: Exception) {

            log.warn("결제 완료 메시지를 보내지 못했습니다. orderId = {}, roomId = {}", payment.getOrderId(), roomId, exception)
        }
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
        val contractUrlHash = hashContractUrl(contractUrl)
        var lastException: Exception? = null

        repeat(CHAIN_RECORD_MAX_ATTEMPTS) { attempt ->

            try {

                return blockchainService.recordPayment(
                    buyerAddress,
                    orderId,
                    amount,
                    paidAt,
                    contractUrlHash,
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

        return MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun hashContractUrl(
        contractUrl: String
    ): String {

        return sha256("${cosmosProperties.contractUrlSalt} | $contractUrl")
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
