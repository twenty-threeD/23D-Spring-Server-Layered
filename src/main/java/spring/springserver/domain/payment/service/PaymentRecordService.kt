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