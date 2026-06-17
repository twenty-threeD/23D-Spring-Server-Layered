package spring.springserver.domain.payment.data.response

import com.fasterxml.jackson.databind.JsonNode

data class PaymentFailureResponse(
    val code: String?,

    val message: String?
) {

    companion object {

        fun of(node: JsonNode): PaymentFailureResponse {

            return PaymentFailureResponse(
                code = node.textOrNull("code"),
                message = node.textOrNull("message")
            )
        }
    }
}
