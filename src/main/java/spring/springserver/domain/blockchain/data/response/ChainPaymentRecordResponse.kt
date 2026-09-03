package spring.springserver.domain.blockchain.data.response

data class ChainPaymentRecordResponse(
    val orderId: String,

    val buyerAddress: String,

    val amount: Long,

    val paidAt: String,

    val contractUrl: String,

    val paymentHash: String,

    val buyerSignature: String,

    val recordedHeight: Long

) {

    companion object {

        fun of(
            record: Map<*, *>
        ): ChainPaymentRecordResponse {

            return ChainPaymentRecordResponse(
                orderId = record["order_id"]?.toString().orEmpty(),
                buyerAddress = record["buyer_address"]?.toString().orEmpty(),
                amount = record["amount"]?.toString()?.toLongOrNull() ?: 0L,
                paidAt = record["paid_at"]?.toString().orEmpty(),
                contractUrl = record["contract_url"]?.toString().orEmpty(),
                paymentHash = record["payment_hash"]?.toString().orEmpty(),
                buyerSignature = record["buyer_signature"]?.toString().orEmpty(),
                recordedHeight = record["recorded_height"]?.toString()?.toLongOrNull() ?: 0L
            )
        }
    }
}