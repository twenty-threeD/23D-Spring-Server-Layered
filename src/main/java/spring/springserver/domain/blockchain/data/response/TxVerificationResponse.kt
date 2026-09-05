package spring.springserver.domain.blockchain.data.response

/**
 * 트랜잭션 해시 하나로 온체인 결제 기록을 검증한 결과다.
 * 실패도 정상 응답(200)으로 내려가며 reason 으로 원인을 구분한다.
 * party 는 조회자가 결제 당사자인지를 나타내며, detail 이 null 인 이유를
 * "당사자가 아님"과 "검증 실패"로 구분하기 위한 값이다.
 */
data class TxVerificationResponse(
    val txHash: String,
    val verified: Boolean,
    val reason: VerificationFailureReason?,
    val reasonMessage: String?,
    val height: Long?,
    val orderId: String?,
    val amount: Long?,
    val paidAt: String?,
    val ledgerMatched: Boolean,
    val signatureValid: Boolean?,
    val party: Boolean,
    val detail: TxVerificationDetailResponse?
) {

    companion object {

        fun fail(
            txHash: String,
            reason: VerificationFailureReason
        ): TxVerificationResponse {

            return TxVerificationResponse(
                txHash = txHash,
                verified = false,
                reason = reason,
                reasonMessage = reason.message,
                height = null,
                orderId = null,
                amount = null,
                paidAt = null,
                ledgerMatched = false,
                signatureValid = null,
                party = false,
                detail = null
            )
        }

        /**
         * tx 를 찾았지만 검증에 실패한 경우다.
         * 무엇이 어긋났는지 보여줘야 하므로 체인에서 읽은 값은 그대로 내려준다.
         */
        fun failWith(
            chainTxResponse: ChainTxResponse,
            reason: VerificationFailureReason,
            ledgerMatched: Boolean,
            signatureValid: Boolean?
        ): TxVerificationResponse {

            return TxVerificationResponse(
                txHash = chainTxResponse.txHash,
                verified = false,
                reason = reason,
                reasonMessage = reason.message,
                height = chainTxResponse.height,
                orderId = chainTxResponse.orderId,
                amount = chainTxResponse.amount,
                paidAt = chainTxResponse.paidAt,
                ledgerMatched = ledgerMatched,
                signatureValid = signatureValid,
                party = false,
                detail = null
            )
        }

        fun of(
            chainPaymentRecordResponse: ChainPaymentRecordResponse,
            txHash: String,
            signatureValid: Boolean,
            party: Boolean,
            txVerificationDetailResponse: TxVerificationDetailResponse?
        ): TxVerificationResponse {

            return TxVerificationResponse(
                txHash = txHash,
                verified = true,
                reason = null,
                reasonMessage = null,
                height = chainPaymentRecordResponse.recordedHeight,
                orderId = chainPaymentRecordResponse.orderId,
                amount = chainPaymentRecordResponse.amount,
                paidAt = chainPaymentRecordResponse.paidAt,
                ledgerMatched = true,
                signatureValid = signatureValid,
                party = party,
                detail = txVerificationDetailResponse
            )
        }
    }
}