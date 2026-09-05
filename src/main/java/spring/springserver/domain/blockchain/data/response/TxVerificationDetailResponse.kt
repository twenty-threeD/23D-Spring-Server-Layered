package spring.springserver.domain.blockchain.data.response

/**
 * 결제 당사자에게만 내려주는 상세 정보다.
 * payment_hash 재계산에는 비공개 값인 paymentKey 가 필요하므로 공개 계층에서는 판정할 수 없다.
 */
data class TxVerificationDetailResponse(
    val contractUrl: String,
    val contractUrlMatched: Boolean,
    val sellerName: String?,
    val buyerName: String?
) {

    companion object {

        fun of(
            contractUrl: String,
            contractUrlMatched: Boolean,
            sellerName: String?,
            buyerName: String?
        ): TxVerificationDetailResponse {

            return TxVerificationDetailResponse(
                contractUrl = contractUrl,
                contractUrlMatched = contractUrlMatched,
                sellerName = sellerName,
                buyerName = buyerName
            )
        }
    }
}
