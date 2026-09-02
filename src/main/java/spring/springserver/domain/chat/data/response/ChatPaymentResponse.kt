package spring.springserver.domain.chat.data.response

data class ChatPaymentResponse(
    val orderId: String,
    val orderName: String,
    val amount: Long,
    val paymentHash: String,
    val blockchainTxHash: String?
) {

    companion object {

        fun of(
            orderId: String,
            orderName: String,
            amount: Long,
            paymentHash: String,
            blockchainTxHash: String?
        ): ChatPaymentResponse {

            return ChatPaymentResponse(
                orderId = orderId,
                orderName = orderName,
                amount = amount,
                paymentHash = paymentHash,
                blockchainTxHash = blockchainTxHash
            )
        }
    }
}