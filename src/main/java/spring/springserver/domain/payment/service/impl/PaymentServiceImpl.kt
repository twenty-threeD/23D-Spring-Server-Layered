package spring.springserver.domain.payment.service.impl

import org.springframework.stereotype.Service
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.request.VirtualAccountRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.service.PaymentService
import java.security.MessageDigest

@Service
class PaymentServiceImpl(
    private val tossPaymentsClient: TossPaymentsClient,
    private val keyService: KeyService,
    private val blockchainService: BlockchainService,
    private val estimateService: EstimateService
) : PaymentService {

    override fun confirm(
        confirmPaymentRequest: ConfirmPaymentRequest,
        memberId: Long
    ): PaymentResponse {

        /**
         * 견적서 결제라면 승인을 요청하기 전에 금액부터 대조한다.
         * 조작된 금액으로 실제 결제가 일어나는 것을 막는다.
         */
        confirmPaymentRequest.estimateId?.let { estimateId ->

            estimateService.validatePayable(
                estimateId,
                memberId,
                confirmPaymentRequest.amount
            )
        }

        val response = tossPaymentsClient.confirm(confirmPaymentRequest)

        /**
         * 승인된 실제 금액으로 한 번 더 대조한 뒤 결제 완료 처리한다.
         * 이후 해당 견적서는 조회만 가능하다.
         */
        confirmPaymentRequest.estimateId?.let { estimateId ->

            estimateService.markAsPaid(
                estimateId,
                memberId,
                response.totalAmount ?: confirmPaymentRequest.amount
            )
        }

        val hash = sha256("${response.paymentKey} | ${response.orderId} | ${response.totalAmount} | ${response.approvedAt}")
        val buyerSignature = keyService.signHash(memberId, hash)
        val buyerAddress = keyService.deriveCosmosAddress(memberId)
        val buyerPubKeyBase64 = keyService.getPublicKeyBase64(memberId)

        blockchainService.recordPayment(
            buyerAddress,
            buyerPubKeyBase64,
            response.orderId!!,
            hash,
            buyerSignature
        )

        return response
    }

    private fun sha256(
        input: String
    ): String {

        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
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
