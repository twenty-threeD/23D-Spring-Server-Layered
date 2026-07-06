package spring.springserver.domain.payment.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.client.RestTemplate
import spring.springserver.domain.key.entity.MemberKey
import spring.springserver.domain.key.repository.KeyRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.ConfirmPaymentRequest
import spring.springserver.domain.payment.data.response.PaymentResponse

@SpringBootTest
class PaymentBlockchainIntegrationTest {

    @MockBean
    lateinit var tossPaymentsClient: TossPaymentsClient

    @MockBean
    lateinit var keyRepository: KeyRepository

    @Autowired
    lateinit var paymentService: PaymentService

    private val restTemplate = RestTemplate()

    // 테스트용 고정 키쌍 (secp256k1)
    private val buyerPrivKeyHex = "ab3fc420efc19f4c548be89b5d56533fdc42a290b62d2e9edae19357b08b520b"
    private val buyerPubKeyHex  = "039d2c2bef52d7560c31328e9cb4049fc16a1df9138d496faab2f885ed54fe05bb"
    private val testMemberId    = 1L
    private val testOrderId     = "TEST-ORDER-${System.currentTimeMillis()}"

    @BeforeEach
    fun setUp() {

        val fakeMember = Member(
            username = "testuser",
            name = "테스트유저",
            email = "test@test.com",
            phone = null,
            password = "password",
            provider = Provider.AUTH,
            role = Role.USER
        )

        val fakeMemberKey = MemberKey(
            member = fakeMember,
            privateKey = buyerPrivKeyHex,
            publicKey = buyerPubKeyHex
        )

        given(keyRepository.findByMemberId(testMemberId)).willReturn(fakeMemberKey)

        given(tossPaymentsClient.confirm(any())).willReturn(
            PaymentResponse(
                paymentKey = "test-payment-key-001",
                orderId = testOrderId,
                orderName = "테스트 상품",
                method = "카드",
                status = "DONE",
                totalAmount = 10000L,
                balanceAmount = 10000L,
                requestedAt = "2024-06-18T18:00:00+09:00",
                approvedAt = "2024-06-18T18:00:01+09:00",
                isPartialCancelable = true,
                card = null,
                virtualAccount = null,
                cancels = null,
                receiptUrl = null,
                checkoutUrl = null,
                failure = null,
                secret = null
            )
        )
    }

    @Test
    fun `결제 확정 후 체인에 해시가 기록된다`() {

        val request = ConfirmPaymentRequest(
            paymentKey = "test-payment-key-001",
            orderId = testOrderId,
            amount = 10000L
        )

        val response = paymentService.confirm(
            request,
            testMemberId
        )

        assertThat(response.status).isEqualTo("DONE")

        // 체인에서 결제 기록 조회
        Thread.sleep(8000)

        val chainResult = restTemplate.getForObject(
            "http://localhost:1317/cosmos/staking/v1beta1/poa/payment/$testOrderId",
            Map::class.java
        )

        println("=== 체인 기록 결과 ===")
        println(chainResult)

        assertThat(chainResult).isNotNull
        assertThat(chainResult!!["submission_id"]).isEqualTo(testOrderId)
    }
}
