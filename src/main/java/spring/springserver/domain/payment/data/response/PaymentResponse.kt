package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode

data class PaymentResponse(
    val paymentKey: String?,

    val orderId: String?,

    val orderName: String?,

    val method: String?,

    val status: String?,

    val totalAmount: Long?,

    val balanceAmount: Long?,

    val requestedAt: String?,

    val approvedAt: String?,

    val isPartialCancelable: Boolean?,

    val card: PaymentCardResponse?,

    val virtualAccount: PaymentVirtualAccountResponse?,

    val cancels: List<PaymentCancelResponse>?,

    val receiptUrl: String?,

    val checkoutUrl: String?,

    val failure: PaymentFailureResponse?,

    @get:JsonIgnore
    val secret: String?
) {

    companion object {

        fun of(node: JsonNode): PaymentResponse {

            return PaymentResponse(
                paymentKey = node.textOrNull("paymentKey"),
                orderId = node.textOrNull("orderId"),
                orderName = node.textOrNull("orderName"),
                method = node.textOrNull("method"),
                status = node.textOrNull("status"),
                totalAmount = node.longOrNull("totalAmount"),
                balanceAmount = node.longOrNull("balanceAmount"),
                requestedAt = node.textOrNull("requestedAt"),
                approvedAt = node.textOrNull("approvedAt"),
                isPartialCancelable = node.booleanOrNull("isPartialCancelable"),
                card = node.objectOrNull("card", PaymentCardResponse::of),
                virtualAccount = node.objectOrNull("virtualAccount", PaymentVirtualAccountResponse::of),
                cancels = node.listOrNull("cancels", PaymentCancelResponse::of),
                receiptUrl = node.path("receipt").textOrNull("url"),
                checkoutUrl = node.path("checkout").textOrNull("url"),
                failure = node.objectOrNull("failure", PaymentFailureResponse::of),
                secret = node.textOrNull("secret")
            )
        }
    }
}
