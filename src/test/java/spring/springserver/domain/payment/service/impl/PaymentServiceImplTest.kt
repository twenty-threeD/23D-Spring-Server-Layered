package spring.springserver.domain.payment.service.impl

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import spring.springserver.domain.blockchain.exception.BlockchainAlreadyRecordedException
import spring.springserver.domain.blockchain.service.BlockchainService
import spring.springserver.domain.estimate.data.response.EstimateResponse
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.payment.client.TossPaymentsClient
import spring.springserver.domain.payment.data.request.CancelPaymentRequest
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.support.PaymentFixtures

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock
    private lateinit var tossPaymentsClient: TossPaymentsClient

    @Mock
    private lateinit var keyService: KeyService

    @Mock
    private lateinit var blockchainService: BlockchainService

    @Mock
    private lateinit var estimateService: EstimateService

    @Mock
    private lateinit var paymentRecordService: PaymentRecordService

    @InjectMocks
    private lateinit var paymentServiceImpl: PaymentServiceImpl

    @Nested
    @DisplayName("confirm")
    inner class Confirm {

        @Test
        @DisplayName("이미 체인에 기록된 주문이면 토스에 다시 승인을 요청하지 않고 저장된 해시를 돌려준다")
        fun returnsStoredHashWhenAlreadyChainRecorded() {

            val request = PaymentFixtures.confirmPaymentRequest()

            whenever(paymentRecordService.findByOrderId(request.orderId))
                .thenReturn(PaymentFixtures.chainRecordedPayment())
            whenever(tossPaymentsClient.findByOrderId(request.orderId))
                .thenReturn(PaymentFixtures.paymentResponse())

            val response = paymentServiceImpl.confirm(
                request,
                PaymentFixtures.MEMBER_ID
            )

            assertThat(response.paymentHash).isEqualTo(PaymentFixtures.paymentHash())
            assertThat(response.blockchainTxHash).isEqualTo(PaymentFixtures.BLOCKCHAIN_TX_HASH)

            verify(tossPaymentsClient, never()).confirm(any())
            verify(paymentRecordService, never()).startConfirm(
                any(),
                any(),
                any()
            )
        }

        @Test
        @DisplayName("정상 승인되면 결제 완료·체인 기록 상태로 바꾸고 계산한 해시를 돌려준다")
        fun confirmsAndRecordsOnChain() {

            val request = PaymentFixtures.confirmPaymentRequest()
            val expectedHash = PaymentFixtures.paymentHash()

            stubSuccessfulConfirm()

            val response = paymentServiceImpl.confirm(
                request,
                PaymentFixtures.MEMBER_ID
            )

            assertThat(response.paymentHash).isEqualTo(expectedHash)
            assertThat(response.blockchainTxHash).isEqualTo(PaymentFixtures.BLOCKCHAIN_TX_HASH)

            verify(paymentRecordService).markDone(
                request.orderId,
                PaymentFixtures.PAYMENT_KEY
            )
            verify(paymentRecordService).markChainRecorded(
                request.orderId,
                expectedHash,
                PaymentFixtures.BLOCKCHAIN_TX_HASH
            )
        }

        @Test
        @DisplayName("견적서 결제면 승인을 요청하기 전에 금액을 대조하고, 승인 후 결제 완료 처리한다")
        fun validatesEstimateBeforeConfirmAndMarksPaidAfter() {

            val request = PaymentFixtures.confirmPaymentRequest(estimateId = 7L)

            stubSuccessfulConfirm()

            whenever(
                estimateService.markAsPaid(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(org.mockito.kotlin.mock<EstimateResponse>())

            paymentServiceImpl.confirm(
                request,
                PaymentFixtures.MEMBER_ID
            )

            verify(estimateService).validatePayable(
                7L,
                PaymentFixtures.MEMBER_ID,
                PaymentFixtures.AMOUNT
            )
            verify(estimateService).markAsPaid(
                7L,
                PaymentFixtures.MEMBER_ID,
                PaymentFixtures.AMOUNT
            )
        }

        @Test
        @DisplayName("견적서 금액 대조에 실패하면 결제 건을 선점하지 않는다")
        fun doesNotStartConfirmWhenEstimateValidationFails() {

            val request = PaymentFixtures.confirmPaymentRequest(estimateId = 7L)

            whenever(paymentRecordService.findByOrderId(request.orderId))
                .thenReturn(PaymentFixtures.payment())
            whenever(
                estimateService.validatePayable(
                    any(),
                    any(),
                    any()
                )
            ).thenThrow(ApplicationException(PaymentStatusCode.PAYMENT_AMOUNT_MISMATCH))

            assertThatThrownBy {

                paymentServiceImpl.confirm(
                    request,
                    PaymentFixtures.MEMBER_ID
                )
            }
                .isInstanceOf(ApplicationException::class.java)

            verify(paymentRecordService, never()).startConfirm(
                any(),
                any(),
                any()
            )
            verify(tossPaymentsClient, never()).confirm(any())
        }

        @Test
        @DisplayName("토스 승인이 실패하면 결제 건을 ABANDONED로 남기고 예외를 그대로 던진다")
        fun marksAbandonedWhenTossConfirmFails() {

            val request = PaymentFixtures.confirmPaymentRequest()

            whenever(paymentRecordService.findByOrderId(request.orderId))
                .thenReturn(PaymentFixtures.payment())
            whenever(
                paymentRecordService.startConfirm(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(PaymentFixtures.payment())
            whenever(tossPaymentsClient.confirm(any()))
                .thenThrow(ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED))

            assertThatThrownBy {

                paymentServiceImpl.confirm(
                    request,
                    PaymentFixtures.MEMBER_ID
                )
            }
                .isInstanceOf(ApplicationException::class.java)

            verify(paymentRecordService).markAbandoned(
                eq(request.orderId),
                any()
            )
            verify(paymentRecordService, never()).markDone(
                any(),
                any()
            )
        }

        @Test
        @DisplayName("이미 체인에 있는 주문이면 새 트랜잭션 없이 기록 완료로 처리한다")
        fun treatsAlreadyRecordedChainErrorAsSuccess() {

            val request = PaymentFixtures.confirmPaymentRequest()

            stubSuccessfulConfirm()

            whenever(
                blockchainService.recordPayment(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenThrow(BlockchainAlreadyRecordedException("이미 기록된 주문입니다."))

            val response = paymentServiceImpl.confirm(
                request,
                PaymentFixtures.MEMBER_ID
            )

            assertThat(response.blockchainTxHash).isNull()

            verify(paymentRecordService).markChainRecorded(
                request.orderId,
                PaymentFixtures.paymentHash(),
                null
            )
        }

        @Test
        @DisplayName("체인 기록에 실패하면 승인을 보상 취소하고 PAYMENT_BLOCKCHAIN_RECORD_FAILED를 던진다")
        fun compensatesWhenChainRecordFails() {

            val request = PaymentFixtures.confirmPaymentRequest()

            stubSuccessfulConfirm()

            whenever(
                blockchainService.recordPayment(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenThrow(IllegalStateException("chain rejected tx"))

            assertThatThrownBy {

                paymentServiceImpl.confirm(
                    request,
                    PaymentFixtures.MEMBER_ID
                )
            }
                .isInstanceOf(ApplicationException::class.java)
                .extracting { (it as ApplicationException).statusCode }
                .isEqualTo(PaymentStatusCode.PAYMENT_BLOCKCHAIN_RECORD_FAILED)

            verify(tossPaymentsClient).cancel(
                eq(PaymentFixtures.PAYMENT_KEY),
                any<CancelPaymentRequest>(),
                eq(PaymentServiceImpl.cancelIdempotencyKey(request.orderId))
            )
            verify(paymentRecordService).markCanceledByChainFailure(
                eq(request.orderId),
                any()
            )
            verify(paymentRecordService, never()).markChainRecorded(
                any(),
                any(),
                any()
            )
        }

        @Test
        @DisplayName("보상 취소까지 실패하면 CANCEL_PENDING으로 남기고 PAYMENT_CANCEL_FAILED를 던진다")
        fun marksCancelPendingWhenCompensationFails() {

            val request = PaymentFixtures.confirmPaymentRequest()

            stubSuccessfulConfirm()

            whenever(
                blockchainService.recordPayment(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenThrow(IllegalStateException("chain rejected tx"))
            whenever(
                tossPaymentsClient.cancel(
                    any(),
                    any(),
                    any()
                )
            ).thenThrow(ApplicationException(PaymentStatusCode.TOSS_PAYMENTS_REQUEST_FAILED))

            assertThatThrownBy {

                paymentServiceImpl.confirm(
                    request,
                    PaymentFixtures.MEMBER_ID
                )
            }
                .isInstanceOf(ApplicationException::class.java)
                .extracting { (it as ApplicationException).statusCode }
                .isEqualTo(PaymentStatusCode.PAYMENT_CANCEL_FAILED)

            verify(paymentRecordService).markCancelPending(
                eq(request.orderId),
                any()
            )
        }

        private fun stubSuccessfulConfirm() {

            whenever(paymentRecordService.findByOrderId(any()))
                .thenReturn(PaymentFixtures.payment())
            whenever(
                paymentRecordService.startConfirm(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(PaymentFixtures.payment())
            whenever(tossPaymentsClient.confirm(any()))
                .thenReturn(PaymentFixtures.paymentResponse())
            whenever(
                keyService.signHash(
                    any(),
                    any()
                )
            ).thenReturn(PaymentFixtures.BUYER_SIGNATURE)
            whenever(keyService.deriveCosmosAddress(any()))
                .thenReturn(PaymentFixtures.BUYER_ADDRESS)
            whenever(
                blockchainService.recordPayment(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(PaymentFixtures.BLOCKCHAIN_TX_HASH)
        }
    }

    @Nested
    @DisplayName("verify")
    inner class Verify {

        @Test
        @DisplayName("결제한 회원이 아니면 PAYMENT_MEMBER_MISMATCH를 던진다")
        fun throwsWhenMemberMismatch() {

            whenever(paymentRecordService.findByOrderId(any()))
                .thenReturn(PaymentFixtures.payment(memberId = PaymentFixtures.MEMBER_ID))

            assertThatThrownBy {

                paymentServiceImpl.verify(
                    PaymentFixtures.ORDER_ID,
                    PaymentFixtures.MEMBER_ID + 1
                )
            }
                .isInstanceOf(ApplicationException::class.java)
                .extracting { (it as ApplicationException).statusCode }
                .isEqualTo(PaymentStatusCode.PAYMENT_MEMBER_MISMATCH)
        }

        @Test
        @DisplayName("체인에 기록이 없으면 PAYMENT_NOT_RECORDED_ON_CHAIN을 던진다")
        fun throwsWhenNotRecordedOnChain() {

            whenever(paymentRecordService.findByOrderId(any()))
                .thenReturn(PaymentFixtures.payment())
            whenever(blockchainService.findRecord(any()))
                .thenReturn(null)

            assertThatThrownBy {

                paymentServiceImpl.verify(
                    PaymentFixtures.ORDER_ID,
                    PaymentFixtures.MEMBER_ID
                )
            }
                .isInstanceOf(ApplicationException::class.java)
                .extracting { (it as ApplicationException).statusCode }
                .isEqualTo(PaymentStatusCode.PAYMENT_NOT_RECORDED_ON_CHAIN)
        }

        @Test
        @DisplayName("해시·서명·주소·금액이 모두 일치하면 verified가 true다")
        fun verifiesWhenEverythingMatches() {

            whenever(paymentRecordService.findByOrderId(any()))
                .thenReturn(PaymentFixtures.payment())
            whenever(blockchainService.findRecord(any()))
                .thenReturn(PaymentFixtures.chainPaymentRecordResponse())
            whenever(tossPaymentsClient.findByOrderId(any()))
                .thenReturn(PaymentFixtures.paymentResponse())
            whenever(
                keyService.verifySignature(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(true)
            whenever(keyService.deriveCosmosAddress(any()))
                .thenReturn(PaymentFixtures.BUYER_ADDRESS)

            val response = paymentServiceImpl.verify(
                PaymentFixtures.ORDER_ID,
                PaymentFixtures.MEMBER_ID
            )

            assertThat(response.verified).isTrue()
            assertThat(response.hashMatched).isTrue()
            assertThat(response.amountMatched).isTrue()
        }

        @Test
        @DisplayName("체인 해시와 재계산 해시가 다르면 verified가 false다")
        fun failsVerificationWhenHashDiffers() {

            whenever(paymentRecordService.findByOrderId(any()))
                .thenReturn(PaymentFixtures.payment())
            whenever(blockchainService.findRecord(any()))
                .thenReturn(PaymentFixtures.chainPaymentRecordResponse(paymentHash = "tampered"))
            whenever(tossPaymentsClient.findByOrderId(any()))
                .thenReturn(PaymentFixtures.paymentResponse())
            whenever(
                keyService.verifySignature(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(true)
            whenever(keyService.deriveCosmosAddress(any()))
                .thenReturn(PaymentFixtures.BUYER_ADDRESS)

            val response = paymentServiceImpl.verify(
                PaymentFixtures.ORDER_ID,
                PaymentFixtures.MEMBER_ID
            )

            assertThat(response.verified).isFalse()
            assertThat(response.hashMatched).isFalse()
        }
    }
}
