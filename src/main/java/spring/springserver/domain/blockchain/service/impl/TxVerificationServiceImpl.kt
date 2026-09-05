package spring.springserver.domain.blockchain.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.blockchain.data.response.ChainPaymentRecordResponse
import spring.springserver.domain.blockchain.data.response.ChainTxResponse
import spring.springserver.domain.blockchain.data.response.TxVerificationDetailResponse
import spring.springserver.domain.blockchain.data.response.TxVerificationResponse
import spring.springserver.domain.blockchain.data.response.VerificationFailureReason
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.blockchain.service.TxVerificationService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.global.util.ContractUrlHasher

@Service
@Transactional(readOnly = true)
class TxVerificationServiceImpl(
    private val blockchainService: BlockchainService,
    private val keyService: KeyService,
    private val paymentRecordService: PaymentRecordService,
    private val contractUrlHasher: ContractUrlHasher
): TxVerificationService {

    override fun verifyByTxHash(
        txHash: String,
        memberId: Long?
    ): TxVerificationResponse {

        val normalizedTxHash = txHash.trim().uppercase()

        if (!TX_HASH_PATTERN.matches(normalizedTxHash)) {

            return TxVerificationResponse.fail(
                normalizedTxHash,
                VerificationFailureReason.INVALID_TX_HASH
            )
        }

        val chainTxResponse = blockchainService.findTx(txHash = normalizedTxHash)
            ?: return TxVerificationResponse.fail(
                normalizedTxHash,
                VerificationFailureReason.TX_NOT_FOUND
            )

        if (!chainTxResponse.isRecordPaymentMessage()) {

            return TxVerificationResponse.fail(
                normalizedTxHash,
                VerificationFailureReason.NOT_PAYMENT_RECORD
            )
        }

        if (!chainTxResponse.isSucceeded()) {

            return failWith(
                chainTxResponse,
                VerificationFailureReason.TX_FAILED
            )
        }

        val record = blockchainService.findRecord(orderId = chainTxResponse.orderId)
            ?: return failWith(
                chainTxResponse,
                VerificationFailureReason.NOT_ON_LEDGER
            )

        if (!chainTxResponse.matches(record)) {

            return failWith(
                chainTxResponse,
                VerificationFailureReason.LEDGER_MISMATCH
            )
        }

        val payment = paymentRecordService.findByOrderIdOrNull(orderId = record.orderId)
            ?: return TxVerificationResponse.failWith(
                chainTxResponse,
                VerificationFailureReason.SIGNATURE_UNVERIFIABLE,
                ledgerMatched = true,
                signatureValid = null
            )

        val signatureValid = keyService.verifySignature(
            payment.getMemberId(),
            record.paymentHash,
            record.buyerSignature
        )

        if (!signatureValid) {

            return TxVerificationResponse.failWith(
                chainTxResponse,
                VerificationFailureReason.SIGNATURE_INVALID,
                ledgerMatched = true,
                signatureValid = false
            )
        }

        val party = isParty(
            payment = payment,
            memberId = memberId
        )

        val txVerificationDetailResponse = if (party) detailOf(
            payment = payment,
            chainPaymentRecordResponse = record
        ) else null

        return TxVerificationResponse.of(
            chainPaymentRecordResponse = record,
            txHash = normalizedTxHash,
            signatureValid = signatureValid,
            party = party,
            txVerificationDetailResponse = txVerificationDetailResponse
        )
    }

    private fun isParty(
        payment: Payment,
        memberId: Long?
    ): Boolean {

        return memberId != null && payment.getMemberId() == memberId
    }

    private fun detailOf(
        payment: Payment,
        chainPaymentRecordResponse: ChainPaymentRecordResponse
    ): TxVerificationDetailResponse {

        return TxVerificationDetailResponse.of(
            contractUrl = payment.getContractUrl(),
            contractUrlMatched = contractUrlHasher.matches(
                contractUrl = payment.getContractUrl(),
                chainPaymentRecordResponse.contractUrlHash
            )
        )
    }

    private fun failWith(
        chainTxResponse: ChainTxResponse,
        reason: VerificationFailureReason
    ): TxVerificationResponse {

        return TxVerificationResponse.failWith(
            chainTxResponse,
            reason,
            ledgerMatched = false,
            signatureValid = null
        )
    }

    companion object {

        private val TX_HASH_PATTERN = Regex("^[0-9A-F]{64}$")
    }
}
