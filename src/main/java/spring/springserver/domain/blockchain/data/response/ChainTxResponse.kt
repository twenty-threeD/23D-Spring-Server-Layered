package spring.springserver.domain.blockchain.data.response

/**
 * /cosmos/tx/v1beta1/txs/{hash} 응답에서 검증에 필요한 값만 뽑아낸다.
 * 여기 담긴 값은 "제출된 메시지"일 뿐 원장에 반영된 상태가 아니므로,
 * 반드시 원장 조회 결과와 대조한 뒤에 사용해야 한다.
 */
data class ChainTxResponse(
    val txHash: String,
    val height: Long,
    val code: Int,
    val typeUrl: String,
    val orderId: String,
    val buyerAddress: String,
    val amount: Long,
    val paidAt: String,
    val contractUrlHash: String,
    val paymentHash: String,
    val buyerSignature: String
) {

    fun isRecordPaymentMessage() = typeUrl == RECORD_PAYMENT_TYPE_URL
    fun isSucceeded() = code == 0

    /**
     * 원장에 실제로 남은 기록과 tx 본문이 모두 같은지 확인한다.
     * 거부된 tx 나 중복 제출된 tx 로 남의 주문을 증명하는 것을 막는다.
     */
    fun matches(
        chainPaymentRecordResponse: ChainPaymentRecordResponse
    ): Boolean {

        return orderId == chainPaymentRecordResponse.orderId &&
                buyerAddress == chainPaymentRecordResponse.buyerAddress &&
                amount == chainPaymentRecordResponse.amount &&
                paidAt == chainPaymentRecordResponse.paidAt &&
                contractUrlHash == chainPaymentRecordResponse.contractUrlHash &&
                paymentHash == chainPaymentRecordResponse.paymentHash &&
                buyerSignature == chainPaymentRecordResponse.buyerSignature
    }

    companion object {

        const val RECORD_PAYMENT_TYPE_URL = "/itda.payment.v1.MsgRecordPayment"

        fun of(
            txResponse: Map<*, *>
        ): ChainTxResponse? {

            val message = ((txResponse["tx"] as? Map<*, *>)
                ?.get("body") as? Map<*, *>)
                ?.let { it["messages"] as? List<*> }
                ?.firstOrNull() as? Map<*, *>
                ?: return null

            return ChainTxResponse(
                txHash = txResponse["txhash"]?.toString().orEmpty(),
                height = txResponse["height"]?.toString()?.toLongOrNull() ?: 0L,
                code = txResponse["code"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1,
                typeUrl = message["@type"]?.toString().orEmpty(),
                orderId = message["order_id"]?.toString().orEmpty(),
                buyerAddress = message["buyer_address"]?.toString().orEmpty(),
                amount = message["amount"]?.toString()?.toLongOrNull() ?: 0L,
                paidAt = message["paid_at"]?.toString().orEmpty(),
                contractUrlHash = message["contract_url"]?.toString().orEmpty(),
                paymentHash = message["payment_hash"]?.toString().orEmpty(),
                buyerSignature = message["buyer_signature"]?.toString().orEmpty()
            )
        }
    }
}
