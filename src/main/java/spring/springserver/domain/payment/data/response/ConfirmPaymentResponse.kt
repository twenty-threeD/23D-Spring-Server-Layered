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
    val blockchainTxHash: String?
) {

    companion object {

        fun of(
            payment: PaymentResponse,
            paymentHash: String,
            blockchainTxHash: String?
        ): ConfirmPaymentResponse {

            return ConfirmPaymentResponse(
                payment = payment,
                paymentHash = paymentHash,
                blockchainTxHash = blockchainTxHash
            )
        }
    }
}
