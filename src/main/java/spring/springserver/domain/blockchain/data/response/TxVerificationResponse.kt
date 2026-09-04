package spring.springserver.domain.blockchain.data.response

/**
 * 트랜잭션 해시 하나로 온체인 결제 기록을 검증한 결과다.
 * 실패도 정상 응답(200)으로 내려가며 reason 으로 원인을 구분한다.
 */
data class TxVerificationResponse(
    val txHash: String,
    val verified: Boolean,
    val reason: VerificationFailureReason?,
    val height: Long?,
    val orderId: String?,
    val amount: Long?,
    val paidAt: String?,
    val paymentHash: String?,
    val ledgerMatched: Boolean,
    val signatureValid: Boolean?,
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
                height = null,
                orderId = null,
                amount = null,
                paidAt = null,
                paymentHash = null,
                ledgerMatched = false,
                signatureValid = null,
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
                height = chainTxResponse.height,
                orderId = chainTxResponse.orderId,
                amount = chainTxResponse.amount,
                paidAt = chainTxResponse.paidAt,
                paymentHash = chainTxResponse.paymentHash,
                ledgerMatched = ledgerMatched,
                signatureValid = signatureValid,
                detail = null
            )
        }

        fun of(
            chainPaymentRecordResponse: ChainPaymentRecordResponse,
            txHash: String,
            signatureValid: Boolean,
            txVerificationDetailResponse: TxVerificationDetailResponse?
        ): TxVerificationResponse {

            return TxVerificationResponse(
                txHash = txHash,
                verified = true,
                reason = null,
                height = chainPaymentRecordResponse.recordedHeight,
                orderId = chainPaymentRecordResponse.orderId,
                amount = chainPaymentRecordResponse.amount,
                paidAt = chainPaymentRecordResponse.paidAt,
                paymentHash = chainPaymentRecordResponse.paymentHash,
                ledgerMatched = true,
                signatureValid = signatureValid,
                detail = txVerificationDetailResponse
            )
        }
    }
}
