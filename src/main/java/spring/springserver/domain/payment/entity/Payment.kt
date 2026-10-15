package spring.springserver.domain.payment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "payment",
    indexes = [Index(name = "idx_payment_status", columnList = "status")]
)
class Payment(
    @Column(nullable = false, unique = true, length = 64)
    private val orderId: String,
    @Column(nullable = false)
    private val amount: Long,
    @Column(nullable = false)
    private val memberId: Long,
    @Column(nullable = false, length = 2048)
    private val contractUrl: String,
    @Column(length = 100)
    private val orderName: String? = null,
    @Column(name = "room_id")
    private val roomId: Long? = null,
    @Column(name = "contract_id")
    private val contractId: Long? = null,
    /**
     * 결제 금액에서 떼는 플랫폼 수수료. 수수료 정책이 생기기 전의 결제는 null이다.
     */
    @Column(name = "fee_amount")
    private val feeAmount: Long? = null,
    /**
     * 결제 시점에 적용된 수수료율(%). 정책이 바뀌어도 당시 요율을 알 수 있도록 남긴다. 이 컬럼이 생기기 전의 결제는 null이다.
     */
    @Column(name = "fee_rate_percent")
    private val feeRatePercent: Long? = null,
    /**
     * 전문가에게 정산될 금액(결제 금액 - 수수료). 수수료 정책이 생기기 전의 결제는 null이다.
     */
    @Column(name = "settlement_amount")
    private val settlementAmount: Long? = null
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @Version
    private var version: Long? = null

    @Column(length = 200)
    private var paymentKey: String? = null

    @Column(length = 64)
    private var paymentHash: String? = null

    @Column(length = 100)
    private var blockchainTxHash: String? = null

    @Column(length = 200)
    private var failureReason: String? = null

    @Column(nullable = false)
    private var cancelAttemptCount: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private var status: PaymentStatus = PaymentStatus.READY

    @Column(nullable = false)
    private val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    private var updatedAt: LocalDateTime = LocalDateTime.now()

    fun getId() = id
    fun getOrderId() = orderId
    fun getAmount() = amount
    fun getMemberId() = memberId
    fun getContractUrl() = contractUrl
    fun getContractId() = contractId
    fun getFeeAmount() = feeAmount
    fun getFeeRatePercent() = feeRatePercent
    fun getSettlementAmount() = settlementAmount
    fun getOrderName() = orderName
    fun getRoomId() = roomId
    fun getPaymentKey() = paymentKey
    fun getPaymentHash() = paymentHash
    fun getBlockchainTxHash() = blockchainTxHash
    fun getFailureReason() = failureReason
    fun getCancelAttemptCount() = cancelAttemptCount
    fun getStatus() = status
    fun getCreatedAt() = createdAt
    fun getUpdatedAt() = updatedAt

    fun markInProgress() {

        transitTo(PaymentStatus.IN_PROGRESS)
    }

    fun markDone(
        paymentKey: String
    ) {

        this.paymentKey = paymentKey

        transitTo(PaymentStatus.DONE)
    }

    fun markChainRecorded(
        paymentHash: String,
        blockchainTxHash: String?
    ) {

        this.paymentHash = paymentHash

        if (blockchainTxHash != null) this.blockchainTxHash = blockchainTxHash

        transitTo(PaymentStatus.CHAIN_RECORDED)
    }

    fun markCanceledByChainFailure(
        failureReason: String?
    ) {

        this.failureReason = failureReason?.take(200)

        transitTo(PaymentStatus.CANCELED_BY_CHAIN_FAILURE)
    }

    fun markCancelPending(
        failureReason: String?
    ) {

        this.failureReason = failureReason?.take(200)
        this.cancelAttemptCount += 1

        transitTo(PaymentStatus.CANCEL_PENDING)
    }

    fun markCancelFailed() {

        transitTo(PaymentStatus.CANCEL_FAILED)
    }

    fun markCanceled() {

        transitTo(PaymentStatus.CANCELED)
    }

    fun markPartialCanceled() {

        transitTo(PaymentStatus.PARTIAL_CANCELED)
    }

    /**
     * 의뢰인이 직접 취소할 수 있는 건 체인 기록까지 끝난 결제, 또는 일부만 취소된 결제뿐이다.
     */
    fun isCancelable() = status == PaymentStatus.CHAIN_RECORDED || status == PaymentStatus.PARTIAL_CANCELED

    fun markAbandoned(
        failureReason: String?
    ) {

        this.failureReason = failureReason?.take(200)

        transitTo(PaymentStatus.ABANDONED)
    }

    private fun transitTo(
        status: PaymentStatus
    ) {

        this.status = status
        this.updatedAt = LocalDateTime.now()
    }
}