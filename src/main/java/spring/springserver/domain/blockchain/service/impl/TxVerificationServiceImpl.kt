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
import spring.springserver.domain.contract.data.response.ContractPartyResponse
import spring.springserver.domain.contract.service.ContractService
import spring.springserver.domain.estimate.service.EstimateService
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
    private val contractUrlHasher: ContractUrlHasher,
    private val estimateService: EstimateService,
    private val contractService: ContractService
): TxVerificationService {

    override fun verifyByTxHash(
        txHash: String,
        memberId: Long?
    ): TxVerificationResponse {

        val normalizedTxHash = txHash.trim().uppercase()

        if (!TX_HASH_PATTERN.matches(normalizedTxHash)) {

            return TxVerificationResponse.fail(
                normalizedTxHash.take(MAX_TX_HASH_LENGTH),
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

        /**
         * 검증 성공 여부와 당사자 여부는 별개다.
         * 검증이 실패해도 당사자에게는 자기 결제의 상세를 보여줘야 하므로
         * payment 조회와 party 판정을 검증 단계보다 먼저 끝낸다.
         */
        val payment = paymentRecordService.findByOrderIdOrNull(orderId = chainTxResponse.orderId)
        val contractPartyResponse = payment?.getContractId()?.let {

            contractService.findPartyById(contractId = it)
        }

        val party = payment != null && isParty(
            payment = payment,
            contractPartyResponse = contractPartyResponse,
            memberId = memberId
        )

        if (!chainTxResponse.isSucceeded()) {

            return failWith(
                chainTxResponse = chainTxResponse,
                reason = VerificationFailureReason.TX_FAILED,
                ledgerMatched = false,
                signatureValid = null,
                party = party,
                payment = payment,
                contractPartyResponse = contractPartyResponse,
                chainPaymentRecordResponse = null
            )
        }

        val record = blockchainService.findRecord(orderId = chainTxResponse.orderId)
            ?: return failWith(
                chainTxResponse = chainTxResponse,
                reason = VerificationFailureReason.NOT_ON_LEDGER,
                ledgerMatched = false,
                signatureValid = null,
                party = party,
                payment = payment,
                contractPartyResponse = contractPartyResponse,
                chainPaymentRecordResponse = null
            )

        if (!chainTxResponse.matches(record)) {

            return failWith(
                chainTxResponse = chainTxResponse,
                reason = VerificationFailureReason.LEDGER_MISMATCH,
                ledgerMatched = false,
                signatureValid = null,
                party = party,
                payment = payment,
                contractPartyResponse = contractPartyResponse,
                chainPaymentRecordResponse = record
            )
        }

        /**
         * payment 를 못 찾으면 서명을 재계산할 수 없고 당사자 판정도 불가능하다.
         */
        if (payment == null) {

            return failWith(
                chainTxResponse = chainTxResponse,
                reason = VerificationFailureReason.SIGNATURE_UNVERIFIABLE,
                ledgerMatched = true,
                signatureValid = null,
                party = false,
                payment = null,
                contractPartyResponse = null,
                chainPaymentRecordResponse = record
            )
        }

        val signatureValid = keyService.verifySignature(
            payment.getMemberId(),
            record.paymentHash,
            record.buyerSignature
        )

        if (!signatureValid) {

            return failWith(
                chainTxResponse = chainTxResponse,
                reason = VerificationFailureReason.SIGNATURE_INVALID,
                ledgerMatched = true,
                signatureValid = false,
                party = party,
                payment = payment,
                contractPartyResponse = contractPartyResponse,
                chainPaymentRecordResponse = record
            )
        }

        return TxVerificationResponse.of(
            chainPaymentRecordResponse = record,
            txHash = normalizedTxHash,
            signatureValid = signatureValid,
            party = party,
            txVerificationDetailResponse = detailOf(
                payment = payment,
                party = party,
                chainPaymentRecordResponse = record,
                contractPartyResponse = contractPartyResponse
            )
        )
    }

    private fun isParty(
        payment: Payment,
        contractPartyResponse: ContractPartyResponse?,
        memberId: Long?
    ): Boolean {

        if (memberId == null) return false

        if (payment.getMemberId() == memberId) return true

        if (contractPartyResponse != null
            && (contractPartyResponse.clientId == memberId || contractPartyResponse.professionalId == memberId)
        ) return true

        val estimateId = payment.getEstimateId()
            ?: return false

        return estimateService.isProfessional(
            estimateId = estimateId,
            memberId = memberId
        )
    }

    /**
     * 원장 기록이 없는 실패 경로에서는 대조할 해시가 없으므로
     * contractUrlMatched 를 false 가 아니라 null(판정 불가)로 둔다.
     */
    private fun detailOf(
        payment: Payment?,
        party: Boolean,
        chainPaymentRecordResponse: ChainPaymentRecordResponse?,
        contractPartyResponse: ContractPartyResponse?
    ): TxVerificationDetailResponse? {

        if (!party || payment == null) return null

        val contractUrl = payment.getContractUrl()

        return TxVerificationDetailResponse.of(
            contractUrl = contractUrl,
            contractUrlMatched = chainPaymentRecordResponse?.let {

                contractUrlHasher.matches(
                    contractUrl = contractUrl,
                    it.contractUrlHash
                )
            },
            sellerName = contractPartyResponse?.professionalName,
            buyerName = contractPartyResponse?.clientName
        )
    }

    private fun failWith(
        chainTxResponse: ChainTxResponse,
        reason: VerificationFailureReason,
        ledgerMatched: Boolean,
        signatureValid: Boolean?,
        party: Boolean,
        payment: Payment?,
        contractPartyResponse: ContractPartyResponse?,
        chainPaymentRecordResponse: ChainPaymentRecordResponse?
    ): TxVerificationResponse {

        return TxVerificationResponse.failWith(
            chainTxResponse,
            reason,
            ledgerMatched = ledgerMatched,
            signatureValid = signatureValid,
            party = party,
            txVerificationDetailResponse = detailOf(
                payment = payment,
                party = party,
                chainPaymentRecordResponse = chainPaymentRecordResponse,
                contractPartyResponse = contractPartyResponse
            )
        )
    }

    companion object {

        private const val MAX_TX_HASH_LENGTH = 64
        private val TX_HASH_PATTERN = Regex("^[0-9A-F]{$MAX_TX_HASH_LENGTH}$")
    }
}
