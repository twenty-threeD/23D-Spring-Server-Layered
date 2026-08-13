package spring.springserver.domain.payment.data.response

import spring.springserver.domain.payment.entity.Payment

data class PreparePaymentResponse(
    val orderId: String,

    val amount: Long,

    val orderName: String?,

    val contractUrl: String,

    val status: String
) {

    companion object {

        fun of(
            payment: Payment
        ): PreparePaymentResponse {

            return PreparePaymentResponse(
                orderId = payment.getOrderId(),
                amount = payment.getAmount(),
                orderName = payment.getOrderName(),
                contractUrl = payment.getContractUrl(),
                status = payment.getStatus().name
            )
        }
    }
}