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

    /**
     * 결제를 시작한 채팅방이다. 승인 후 결제 완료 메시지를 보낼 대상이며 없을 수 있다.
     */
    @Column(name = "room_id")
    private val roomId: Long? = null
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

        /**
         * 이미 체인에 기록된 주문을 다시 처리하면 새 트랜잭션이 없어 null이 넘어온다.
         * 먼저 확보해 둔 해시를 지우지 않도록 값이 있을 때만 덮어쓴다.
         */
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