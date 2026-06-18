package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.databind.JsonNode

data class PaymentCardResponse(
    val amount: Long?,

    val issuerCode: String?,

    val acquirerCode: String?,

    val number: String?,

    val installmentPlanMonths: Int?,

    val approveNo: String?
) {

    companion object {

        fun of(node: JsonNode): PaymentCardResponse {

            return PaymentCardResponse(
                amount = node.longOrNull("amount"),
                issuerCode = node.textOrNull("issuerCode"),
                acquirerCode = node.textOrNull("acquirerCode"),
                number = node.textOrNull("number"),
                installmentPlanMonths = node.intOrNull("installmentPlanMonths"),
                approveNo = node.textOrNull("approveNo")
            )
        }
    }
}
