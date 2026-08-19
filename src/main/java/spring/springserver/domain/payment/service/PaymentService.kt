package spring.springserver.domain.payment.service

import spring.springserver.domain.blockchain.data.response.PaymentVerificationResponse
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.data.response.PreparePaymentResponse

interface PaymentService {

    fun prepare(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long
    ): PreparePaymentResponse

    fun confirm(
        confirmPaymentRequest: ConfirmPaymentRequest,
        memberId: Long
        ): PaymentResponse

    fun findByPaymentKey(
        paymentKey: String
    ): PaymentResponse

    fun findByOrderId(
        orderId: String
    ): PaymentResponse

    fun cancel(
        paymentKey: String,
        cancelPaymentRequest: CancelPaymentRequest,
        idempotencyKey: String?
    ): PaymentResponse

    fun issueVirtualAccount(
        virtualAccountRequest: VirtualAccountRequest
    ): PaymentResponse

    fun verify(
        orderId: String,
        memberId: Long
    ): PaymentVerificationResponse
}
