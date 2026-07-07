package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.databind.JsonNode

data class PaymentCancelResponse(
    val transactionKey: String?,

    val cancelReason: String?,

    val cancelAmount: Long?,

    val refundableAmount: Long?,

    val canceledAt: String?,

    val cancelStatus: String?
) {

    companion object {

        fun of(node: JsonNode): PaymentCancelResponse {

            return PaymentCancelResponse(
                transactionKey = node.textOrNull("transactionKey"),
                cancelReason = node.textOrNull("cancelReason"),
                cancelAmount = node.longOrNull("cancelAmount"),
                refundableAmount = node.longOrNull("refundableAmount"),
                canceledAt = node.textOrNull("canceledAt"),
                cancelStatus = node.textOrNull("cancelStatus")
            )
        }
    }
}
