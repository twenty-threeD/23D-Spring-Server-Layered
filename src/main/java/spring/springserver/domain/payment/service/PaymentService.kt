package spring.springserver.domain.payment.service

import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse

interface PaymentService {

    fun confirm(confirmPaymentRequest: ConfirmPaymentRequest): PaymentResponse

    fun findByPaymentKey(paymentKey: String): PaymentResponse

    fun findByOrderId(orderId: String): PaymentResponse

    fun cancel(
        paymentKey: String,
        cancelPaymentRequest: CancelPaymentRequest,
        idempotencyKey: String?
    ): PaymentResponse

    fun issueVirtualAccount(virtualAccountRequest: VirtualAccountRequest): PaymentResponse
}
