package spring.springserver.support

import spring.springserver.domain.blockchain.data.response.ChainPaymentRecordResponse
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.response.PaymentResponse
import spring.springserver.domain.payment.entity.Payment
import java.security.MessageDigest

/**
 * 결제 도메인 단위 테스트에서 반복해 쓰는 값·객체를 한 곳에 모아 둔다.
 * 각 테스트는 검증에 필요한 값만 인자로 덮어쓰고 나머지는 기본값을 그대로 쓴다.
 */
object PaymentFixtures {

    const val ORDER_ID = "order-20260101-0001"
    const val PAYMENT_KEY = "test_payment_key"
    const val MEMBER_ID = 1L
    const val AMOUNT = 10_000L
    const val CONTRACT_URL = "https://example.com/contract.pdf"
    const val APPROVED_AT = "2026-01-01T00:00:00+09:00"
    const val BUYER_ADDRESS = "cosmos1buyeraddress"
    const val BUYER_SIGNATURE = "test_signature"
    const val BLOCKCHAIN_TX_HASH = "TXHASH0001"

    fun payment(
        orderId: String = ORDER_ID,
        amount: Long = AMOUNT,
        memberId: Long = MEMBER_ID,
        contractUrl: String = CONTRACT_URL,
        orderName: String? = "테스트 주문"
    ): Payment {

        return Payment(
            orderId,
            amount,
            memberId,
            contractUrl,
            orderName
        )
    }

    /**
     * READY → IN_PROGRESS → DONE → CHAIN_RECORDED 로 실제 상태 전이를 거친 결제 건을 만든다.
     * 엔티티에 setter가 없으므로 상태별 픽스처는 전이 메서드를 통해서만 만든다.
     */
    fun chainRecordedPayment(
        orderId: String = ORDER_ID,
        memberId: Long = MEMBER_ID,
        paymentHash: String? = paymentHash(),
        blockchainTxHash: String? = BLOCKCHAIN_TX_HASH
    ): Payment {

        val payment = payment(
            orderId = orderId,
            memberId = memberId
        )

        payment.markInProgress()
        payment.markDone(PAYMENT_KEY)

        paymentHash?.let {

            payment.markChainRecorded(
                it,
                blockchainTxHash
            )
        }

        return payment
    }

    fun confirmPaymentRequest(
        paymentKey: String = PAYMENT_KEY,
        orderId: String = ORDER_ID,
        amount: Long = AMOUNT,
        estimateId: Long? = null
    ): ConfirmPaymentRequest {

        return ConfirmPaymentRequest(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amount,
            estimateId = estimateId
        )
    }

    fun paymentResponse(
        paymentKey: String? = PAYMENT_KEY,
        orderId: String? = ORDER_ID,
        totalAmount: Long? = AMOUNT,
        approvedAt: String? = APPROVED_AT,
        status: String? = "DONE"
    ): PaymentResponse {

        return PaymentResponse(
            paymentKey = paymentKey,
            orderId = orderId,
            orderName = "테스트 주문",
            method = "카드",
            status = status,
            totalAmount = totalAmount,
            balanceAmount = totalAmount,
            requestedAt = APPROVED_AT,
            approvedAt = approvedAt,
            isPartialCancelable = true,
            card = null,
            virtualAccount = null,
            cancels = null,
            receiptUrl = null,
            checkoutUrl = null,
            failure = null,
            secret = null
        )
    }

    fun chainPaymentRecordResponse(
        orderId: String = ORDER_ID,
        buyerAddress: String = BUYER_ADDRESS,
        amount: Long = AMOUNT,
        paymentHash: String = paymentHash(),
        buyerSignature: String = BUYER_SIGNATURE
    ): ChainPaymentRecordResponse {

        return ChainPaymentRecordResponse(
            orderId = orderId,
            buyerAddress = buyerAddress,
            amount = amount,
            paidAt = APPROVED_AT,
            contractUrl = CONTRACT_URL,
            paymentHash = paymentHash,
            buyerSignature = buyerSignature,
            recordedHeight = 100L
        )
    }

    /**
     * 서비스가 계산하는 해시와 같은 규칙으로 기대값을 만든다.
     * 규칙이 바뀌면 프로덕션 코드와 함께 여기도 바뀌어야 테스트가 의미를 갖는다.
     */
    fun paymentHash(
        paymentKey: String = PAYMENT_KEY,
        orderId: String = ORDER_ID,
        totalAmount: Long = AMOUNT,
        approvedAt: String = APPROVED_AT
    ): String {

        return sha256("$paymentKey | $orderId | $totalAmount | $approvedAt")
    }

    private fun sha256(
        input: String
    ): String {

        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
