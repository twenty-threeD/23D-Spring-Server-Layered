package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.databind.JsonNode

data class PaymentVirtualAccountResponse(
    val accountNumber: String?,

    val accountType: String?,

    val bankCode: String?,

    val customerName: String?,

    val dueDate: String?,

    val expired: Boolean?,

    val refundStatus: String?
) {

    companion object {

        fun of(node: JsonNode): PaymentVirtualAccountResponse {

            return PaymentVirtualAccountResponse(
                accountNumber = node.textOrNull("accountNumber"),
                accountType = node.textOrNull("accountType"),
                bankCode = node.textOrNull("bankCode"),
                customerName = node.textOrNull("customerName"),
                dueDate = node.textOrNull("dueDate"),
                expired = node.booleanOrNull("expired"),
                refundStatus = node.textOrNull("refundStatus")
            )
        }
    }
}
