package spring.springserver.domain.blockchain.data.response

// 결제 참여자용 상서정보 반환값
data class TxVerificationDetailResponse(
    val contractUrl: String,
    val contractUrlMatched: Boolean?
) {

    companion object {

        fun of(
            contractUrl: String,
            contractUrlMatched: Boolean?,
        ): TxVerificationDetailResponse {

            return TxVerificationDetailResponse(
                contractUrl = contractUrl,
                contractUrlMatched = contractUrlMatched
            )
        }
    }
}
