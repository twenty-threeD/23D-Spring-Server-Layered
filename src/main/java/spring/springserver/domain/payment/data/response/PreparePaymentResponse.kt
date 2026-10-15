package spring.springserver.domain.payment.data.response

import spring.springserver.domain.payment.entity.Payment

data class PreparePaymentResponse(
    val orderId: String,

    val amount: Long,

    val orderName: String?,

    val contractUrl: String,

    val status: String,

    /**
     * 결제 금액에서 떼는 플랫폼 수수료(원).
     */
    val feeAmount: Long?,

    /**
     * 전문가에게 정산될 금액(원). 결제 금액 - 수수료.
     */
    val settlementAmount: Long?
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
                status = payment.getStatus().name,
                feeAmount = payment.getFeeAmount(),
                settlementAmount = payment.getSettlementAmount()
            )
        }
    }
}