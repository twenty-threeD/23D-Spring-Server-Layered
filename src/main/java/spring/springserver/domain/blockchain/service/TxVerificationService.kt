package spring.springserver.domain.blockchain.service

import spring.springserver.domain.blockchain.data.response.TxVerificationResponse

interface TxVerificationService {

    fun verifyByTxHash(
        txHash: String,
        memberId: Long?
    ): TxVerificationResponse
}
