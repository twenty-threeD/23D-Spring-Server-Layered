package spring.springserver.domain.payment.service.impl

import org.springframework.stereotype.Service
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.service.PaymentService

@Service
class PaymentServiceImpl(
    private val tossPaymentsClient: TossPaymentsClient
) : PaymentService {

    override fun confirm(confirmPaymentRequest: ConfirmPaymentRequest): PaymentResponse {

        return tossPaymentsClient.confirm(confirmPaymentRequest)
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
}
