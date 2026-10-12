package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.data.response.PaymentVerificationResponse
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.exception.BlockchainCommitTimeoutException
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.contract.exception.ContractStatusCode
import spring.springserver.domain.contract.service.ContractService
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
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.ContractUrlHasher
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class PaymentServiceImpl(
    private val tossPaymentsClient: TossPaymentsClient,
    private val keyService: KeyService,
    private val blockchainService: BlockchainService,
    private val estimateService: EstimateService,
    private val paymentRecordService: PaymentRecordService,
    private val chatService: ChatService,
    private val contractUrlHasher: ContractUrlHasher,
    private val contractService: ContractService,
    private val redisTemplate: RedisTemplate<String, String>
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

        /**
         * 결제 금액은 클라이언트가 정하는 값이 아니다.
         * 견적서나 계약서라는 서버 측 금액 원천이 반드시 있어야 하고, 요청 금액은 그 값과 정확히 같아야 한다.
         */
        if (preparePaymentRequest.estimateId == null && preparePaymentRequest.contractId == null) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_AMOUNT_SOURCE_REQUIRED)
        }

        preparePaymentRequest.contractId?.let { contractId ->

            val contractPartyResponse = contractService.findPartyById(contractId = contractId)
                ?: throw ApplicationException(ContractStatusCode.CONTRACT_NOT_FOUND)

            if (contractPartyResponse.clientId != memberId
                && contractPartyResponse.professionalId != memberId) {

                throw ApplicationException(PaymentStatusCode.PAYMENT_CONTRACT_FORBIDDEN)
            }

            if (contractPartyResponse.price != preparePaymentRequest.amount) {

                throw ApplicationException(PaymentStatusCode.PAYMENT_AMOUNT_MISMATCH)
            }
        }

        preparePaymentRequest.estimateId?.let { estimateId ->

            estimateService.validatePayable(
                estimateId,
                memberId,
                preparePaymentRequest.amount
            )
        }

        /**
         * 계약서에는 더 이상 PDF 경로가 없다. 원장에 해시로 올릴 경로는 요청 값에서 받는다.
         * contractId는 요청자가 그 계약의 당사자인지 확인하는 용도로만 쓴다.
         */
        val contractUrl = preparePaymentRequest.contractUrl

        return PreparePaymentResponse.of(
            paymentRecordService.create(
                preparePaymentRequest,
                memberId,
                contractUrl
            )
        )
    }

    override fun confirm(
        confirmPaymentRequest: ConfirmPaymentRequest,
        memberId: Long
    ): ConfirmPaymentResponse {

        val stored = paymentRecordService.findByOrderId(confirmPaymentRequest.orderId)

        /**
         * 이미 기록된 주문은 아래에서 토스 결제 조회 결과를 그대로 돌려준다.
         * 그 전에 주인을 확인하지 않으면 남의 orderId만으로 결제 정보를 받아볼 수 있다.
         */
        validateOwner(
            stored,
            memberId
        )

        if (stored.getStatus() == PaymentStatus.CHAIN_RECORDED) {

            return ConfirmPaymentResponse.of(
                payment = tossPaymentsClient.findByOrderId(confirmPaymentRequest.orderId),
                paymentHash = stored.getPaymentHash()
                    ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_RECORDED_ON_CHAIN),
                blockchainTxHash = stored.getBlockchainTxHash()
            )
        }

        /**
         * 견적서는 준비 시점에 박아 둔 값만 쓴다.
         * 클라이언트가 보낸 estimateId를 믿으면 그 값을 빼는 것만으로 금액 검증을 건너뛸 수 있다.
         */
        val estimateId = stored.getEstimateId()

        estimateId?.let {

            estimateService.validatePayable(
                it,
                memberId,
                stored.getAmount()
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

        estimateId?.let {

            estimateService.markAsPaid(
                it,
                memberId,
                response.totalAmount ?: payment.getAmount()
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

        validateOwner(
            payment,
            memberId
        )

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
        val buyerAddress = keyService.deriveCosmosAddress(memberId = memberId)
        val contractUrlHash = contractUrlHasher.hash(contractUrl = contractUrl)
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

    override fun findByPaymentKey(
        paymentKey: String,
        memberId: Long
    ): PaymentResponse {

        validateOwner(
            paymentRecordService.findByPaymentKey(paymentKey),
            memberId
        )

        return tossPaymentsClient.findByPaymentKey(paymentKey)
    }

    override fun findByOrderId(
        orderId: String,
        memberId: Long
    ): PaymentResponse {

        validateOwner(
            paymentRecordService.findByOrderId(orderId),
            memberId
        )

        return tossPaymentsClient.findByOrderId(orderId)
    }

    override fun cancel(
        cancelPaymentRequest: CancelPaymentRequest,
        paymentKey: String,
        idempotencyKey: String?,
        memberId: Long
    ): PaymentResponse {

        /**
         * 같은 결제에 취소가 동시에 들어오면 둘 다 취소 가능 상태로 보고 토스를 두 번 부를 수 있다.
         * 결제마다 락을 잡아 한 번에 하나만 처리하고, 락을 잡은 뒤에 상태를 다시 읽어 확인한다.
         */
        val lockKey = CANCEL_LOCK_PREFIX + paymentKey
        val lockToken = UUID.randomUUID().toString()
        val acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey,
            lockToken,
            CANCEL_LOCK_SECONDS,
            TimeUnit.SECONDS
        )
            ?: false

        if (!acquired) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_CANCEL_IN_PROGRESS)
        }

        try {

            return cancelWithLock(
                cancelPaymentRequest,
                paymentKey,
                idempotencyKey,
                memberId
            )
        } finally {

            /**
             * 처리가 락 수명보다 길어지면 다른 요청이 새 락을 잡았을 수 있으므로 내 락일 때만 푼다.
             */
            if (redisTemplate.opsForValue().get(lockKey) == lockToken) {

                redisTemplate.delete(lockKey)
            }
        }
    }

    private fun cancelWithLock(
        cancelPaymentRequest: CancelPaymentRequest,
        paymentKey: String,
        idempotencyKey: String?,
        memberId: Long
    ): PaymentResponse {

        val stored = paymentRecordService.findByPaymentKey(paymentKey)

        validateOwner(
            stored,
            memberId
        )

        if (!stored.isCancelable()) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_CANCELABLE)
        }

        /**
         * 전액 취소는 결제당 한 번뿐이라 서버가 멱등키를 정할 수 있다.
         * 토스 취소는 끝났는데 응답을 못 받아 다시 요청해도 토스가 같은 결과를 돌려준다.
         * 부분 취소는 같은 금액을 정상적으로 여러 번 할 수 있어 클라이언트 멱등키에 맡긴다.
         */
        val effectiveIdempotencyKey = when {

            idempotencyKey != null -> idempotencyKey
            cancelPaymentRequest.cancelAmount == null -> fullCancelIdempotencyKey(paymentKey)
            else -> null
        }

        val response = tossPaymentsClient.cancel(
            paymentKey,
            cancelPaymentRequest,
            effectiveIdempotencyKey
        )

        /**
         * 토스에서 환불이 끝났는데 우리 쪽 상태가 그대로면 전문가에게는 여전히 결제 완료로 보인다.
         * 토스가 돌려준 상태를 기준으로 결제를 취소 상태로 바꾸고, 전액 취소면 견적서도 결제 전으로 되돌린다.
         */
        try {

            val payment = paymentRecordService.markCanceled(
                paymentKey,
                response.status
            )

            if (payment.getStatus() == PaymentStatus.CANCELED) {

                payment.getEstimateId()?.let {

                    estimateService.markAsRefunded(estimateId = it)
                }
            }
        } catch (exception: Exception) {

            /**
             * 환불은 이미 끝났으므로 상태 반영이 실패해도 사용자에게 실패로 응답하지 않는다.
             * 재처리 대상으로 남겨 스케줄러가 토스 상태를 다시 조회해 맞춘다.
             */
            log.error("결제 취소 후 상태를 반영하지 못했습니다. 재처리 대상으로 기록합니다. paymentKey = {}, tossStatus = {}", paymentKey, response.status, exception)

            runCatching {

                redisTemplate.opsForSet().add(
                    UNSYNCED_CANCEL_KEY,
                    paymentKey
                )
            }.onFailure { recordException ->

                log.error("재처리 대상 기록에도 실패했습니다. 수동 확인이 필요합니다. paymentKey = {}", paymentKey, recordException)
            }
        }

        return response
    }

    override fun issueVirtualAccount(
        virtualAccountRequest: VirtualAccountRequest,
        memberId: Long
    ): PaymentResponse {

        /**
         * 가상계좌도 주문번호로 붙는다. 남의 주문에 계좌를 끼워 넣지 못하도록 소유자와 금액을 먼저 확인한다.
         */
        val payment = paymentRecordService.findByOrderId(virtualAccountRequest.orderId)

        validateOwner(
            payment,
            memberId
        )

        if (payment.getAmount() != virtualAccountRequest.amount) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_AMOUNT_MISMATCH)
        }

        return tossPaymentsClient.issueVirtualAccount(virtualAccountRequest)
    }

    /**
     * 결제 건은 orderId·paymentKey만 알면 주소창에서 바꿔 넣을 수 있는 값이다.
     * 토스로 넘기기 전에 요청자가 그 결제의 주인인지 반드시 확인한다.
     */
    private fun validateOwner(
        payment: Payment,
        memberId: Long
    ) {

        if (payment.getMemberId() != memberId) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_MEMBER_MISMATCH)
        }
    }

    companion object {

        const val CHAIN_FAILURE_CANCEL_REASON = "블록체인 기록 실패로 인한 자동 취소"

        private const val CHAIN_RECORD_MAX_ATTEMPTS = 3
        private const val CHAIN_RECORD_RETRY_DELAY_MILLIS = 500L

        const val UNSYNCED_CANCEL_KEY = "payment:cancel:unsynced"

        private const val CANCEL_LOCK_PREFIX = "payment:cancel:lock:"
        private const val CANCEL_LOCK_SECONDS = 60L

        fun fullCancelIdempotencyKey(
            paymentKey: String
        ): String {

            return "cancel-$paymentKey-full"
        }

        fun cancelIdempotencyKey(
            orderId: String
        ): String {

            return "chain-fail-$orderId"
        }
    }
}
