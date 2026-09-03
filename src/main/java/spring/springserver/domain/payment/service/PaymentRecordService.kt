package spring.springserver.domain.payment.service

import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.entity.Payment

interface PaymentRecordService {

    fun create(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long
    ): Payment

    fun startConfirm(
        orderId: String,
        amount: Long,
        memberId: Long
    ): Payment

    fun findByOrderId(
        orderId: String
    ): Payment

    /**
     * 결제 건이 없어도 예외로 보지 않는 조회다.
     * 체인 기록만 있고 로컬 결제 건을 특정할 수 없는 경우를 검증 결과로 표현해야 하는 쪽에서 쓴다.
     */
    fun findByOrderIdOrNull(
        orderId: String
    ): Payment?

    fun markDone(
        orderId: String,
        paymentKey: String
    )

    fun markChainRecorded(
        orderId: String,
        paymentHash: String,
        blockchainTxHash: String?
    )

    fun markCanceledByChainFailure(
        orderId: String,
        failureReason: String?
    )

    fun markCancelPending(
        orderId: String,
        failureReason: String?
    )

    fun markCancelFailed(
        orderId: String
    )

    fun markAbandoned(
        orderId: String,
        failureReason: String?
    )
}