package spring.springserver.domain.blockchain.data.response

// 결제 참여자용 상서정보 반환값
data class TxVerificationDetailResponse(
    val contractUrl: String,
    val contractUrlMatched: Boolean?,
    val txHashMatched: Boolean?,
    val paymentHashMatched: Boolean?,
    val buyerAddressMatched: Boolean?
) {

    companion object {

        fun of(
            contractUrl: String,
            contractUrlMatched: Boolean?,
            txHashMatched: Boolean?,
            paymentHashMatched: Boolean?,
            buyerAddressMatched: Boolean?
        ): TxVerificationDetailResponse {

            return TxVerificationDetailResponse(
                contractUrl = contractUrl,
                contractUrlMatched = contractUrlMatched,
                txHashMatched = txHashMatched,
                paymentHashMatched = paymentHashMatched,
                buyerAddressMatched = buyerAddressMatched
            )
        }
    }
}
