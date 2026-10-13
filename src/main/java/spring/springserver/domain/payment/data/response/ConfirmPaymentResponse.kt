package spring.springserver.domain.payment.data.response

/**
 * 결제 승인 결과에 블록체인 기록 결과를 함께 담아 내려준다.
 * PaymentResponse는 토스 결제 객체를 그대로 옮긴 DTO이므로, 체인에서 나온 값은 이 응답에서 감싼다.
 */
data class ConfirmPaymentResponse(
    val payment: PaymentResponse,

    val paymentHash: String,

    /**
     * 이미 체인에 기록된 주문이면 새로 발생한 트랜잭션이 없으므로 null이다.
     */
    val blockchainTxHash: String?,

    /**
     * 결제 금액에서 떼는 플랫폼 수수료(원). 수수료 정책이 생기기 전의 결제는 null이다.
     */
    val feeAmount: Long?,

    /**
     * 전문가에게 정산될 금액(원). 수수료 정책이 생기기 전의 결제는 null이다.
     */
    val settlementAmount: Long?
) {

    companion object {

        fun of(
            payment: PaymentResponse,
            paymentHash: String,
            blockchainTxHash: String?,
            feeAmount: Long?,
            settlementAmount: Long?
        ): ConfirmPaymentResponse {

            return ConfirmPaymentResponse(
                payment = payment,
                paymentHash = paymentHash,
                blockchainTxHash = blockchainTxHash,
                feeAmount = feeAmount,
                settlementAmount = settlementAmount
            )
        }
    }
}
