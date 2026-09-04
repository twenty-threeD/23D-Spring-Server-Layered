package spring.springserver.domain.blockchain.data.response

/**
 * 결제 당사자에게만 내려주는 상세 정보다.
 * payment_hash 재계산에는 비공개 값인 paymentKey 가 필요하므로 공개 계층에서는 판정할 수 없다.
 */
data class TxVerificationDetailResponse(
    val orderName: String?,
    val contractUrl: String,
    val hashMatched: Boolean,
    val recalculatedHash: String
) {

    companion object {

        fun of(
            orderName: String?,
            contractUrl: String,
            hashMatched: Boolean,
            recalculatedHash: String
        ): TxVerificationDetailResponse {

            return TxVerificationDetailResponse(
                orderName,
                contractUrl,
                hashMatched,
                recalculatedHash
            )
        }
    }
}
