package spring.springserver.domain.blockchain.data.response

data class PaymentVerificationResponse(
    val orderId: String,

    val verified: Boolean,

    val hashMatched: Boolean,

    val signatureValid: Boolean,

    val buyerAddressMatched: Boolean,

    val amountMatched: Boolean,

    val recordedHeight: Long,

    val chainPaymentHash: String,

    val recalculatedHash: String
) {

    companion object {

        fun of(
            chainPaymentRecordResponse: ChainPaymentRecordResponse,
            recalculatedHash: String,
            signatureValid: Boolean,
            buyerAddressMatched: Boolean,
            amountMatched: Boolean
        ): PaymentVerificationResponse {

            val hashMatched = chainPaymentRecordResponse.paymentHash == recalculatedHash

            return PaymentVerificationResponse(
                orderId = chainPaymentRecordResponse.orderId,
                verified = hashMatched && signatureValid && buyerAddressMatched && amountMatched,
                hashMatched = hashMatched,
                signatureValid = signatureValid,
                buyerAddressMatched = buyerAddressMatched,
                amountMatched = amountMatched,
                recordedHeight = chainPaymentRecordResponse.recordedHeight,
                chainPaymentHash = chainPaymentRecordResponse.paymentHash,
                recalculatedHash = recalculatedHash
            )
        }
    }
}
