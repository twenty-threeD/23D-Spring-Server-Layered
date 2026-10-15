package spring.springserver.domain.payment.service

import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.entity.Payment

interface PaymentRecordService {

    fun create(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long,
        contractUrl: String
    ): Payment

    fun startConfirm(
        orderId: String,
        amount: Long,
        memberId: Long
    ): Payment

    fun findByOrderId(
        orderId: String
    ): Payment

    fun findByPaymentKey(
        paymentKey: String
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

    /**
     * 체인 커밋 결과가 미확정인 결제를 유예 상태로 남긴다.
     * 뒤늦게 커밋될 수 있으므로 바로 취소하지 않고 스케줄러가 원장을 다시 확인한다.
     */
    fun markChainUnconfirmed(
        orderId: String,
        paymentHash: String,
        blockchainTxHash: String?,
        failureReason: String?
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

    /**
     * 토스 취소 응답을 받아 전액 취소와 부분 취소를 구분해 반영한다.
     * 환불 때 수수료는 돌려주지 않으므로, 토스에 수수료만큼 잔액이 남아 있어도
     * 환불할 수 있는 금액을 모두 돌려줬다면 전액 취소로 본다.
     */
    fun markCanceled(
        paymentKey: String,
        tossStatus: String?,
        balanceAmount: Long?
    ): Payment

    fun markAbandoned(
        orderId: String,
        failureReason: String?
    )
}