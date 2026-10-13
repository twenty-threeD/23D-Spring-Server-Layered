package spring.springserver.domain.payment.service.impl

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.domain.payment.policy.PaymentFeePolicy
import spring.springserver.domain.payment.repository.PaymentRepository
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class], propagation = Propagation.REQUIRES_NEW)
class PaymentRecordServiceImpl(
    private val paymentRepository: PaymentRepository
): PaymentRecordService {

    private val log = LoggerFactory.getLogger(PaymentRecordServiceImpl::class.java)

    override fun create(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long,
        contractUrl: String
    ): Payment {

        val payment = Payment(
            orderId = preparePaymentRequest.orderId,
            amount = preparePaymentRequest.amount,
            memberId = memberId,
            contractUrl = contractUrl,
            orderName = preparePaymentRequest.orderName,
            roomId = preparePaymentRequest.roomId,
            contractId = preparePaymentRequest.contractId,
            feeAmount = PaymentFeePolicy.calculateFee(preparePaymentRequest.amount),
            settlementAmount = PaymentFeePolicy.calculateSettlement(preparePaymentRequest.amount)
        )

        try {

            return paymentRepository.saveAndFlush(payment)
        } catch (_: DataIntegrityViolationException) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_ORDER_ID_DUPLICATED)
        }
    }

    override fun startConfirm(
        orderId: String,
        amount: Long,
        memberId: Long
    ): Payment {

        val payment = paymentRepository.findByOrderIdForUpdate(orderId)
            ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_FOUND)

        if (payment.getMemberId() != memberId) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_MEMBER_MISMATCH)
        }

        if (payment.getAmount() != amount) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_AMOUNT_MISMATCH)
        }

        if (payment.getStatus() != PaymentStatus.READY) {

            throw ApplicationException(PaymentStatusCode.PAYMENT_ALREADY_PROCESSED)
        }

        payment.markInProgress()

        return payment
    }

    @Transactional(readOnly = true)
    override fun findByPaymentKey(
        paymentKey: String
    ): Payment {

        return paymentRepository.findByPaymentKey(paymentKey)
            ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_FOUND)
    }

    @Transactional(readOnly = true)
    override fun findByOrderId(
        orderId: String
    ): Payment {

        return paymentRepository.findByOrderId(orderId)
            ?: throw ApplicationException(PaymentStatusCode.PAYMENT_NOT_FOUND)
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    override fun findByOrderIdOrNull(
        orderId: String
    ): Payment? {

        return paymentRepository.findByOrderId(orderId)
    }

    override fun markDone(
        orderId: String,
        paymentKey: String
    ) {

        findByOrderId(orderId).markDone(paymentKey)
    }

    override fun markChainRecorded(
        orderId: String,
        paymentHash: String,
        blockchainTxHash: String?
    ) {

        findByOrderId(orderId).markChainRecorded(
            paymentHash,
            blockchainTxHash
        )
    }

    override fun markCanceledByChainFailure(
        orderId: String,
        failureReason: String?
    ) {

        findByOrderId(orderId).markCanceledByChainFailure(failureReason)
    }

    override fun markCancelPending(
        orderId: String,
        failureReason: String?
    ) {

        findByOrderId(orderId).markCancelPending(failureReason)
    }

    override fun markCancelFailed(
        orderId: String
    ) {

        findByOrderId(orderId).markCancelFailed()
    }

    override fun markCanceled(
        paymentKey: String,
        tossStatus: String?,
        balanceAmount: Long?
    ): Payment {

        val payment = findByPaymentKey(paymentKey)
        val feeAmount = payment.getFeeAmount() ?: 0L
        val onlyFeeRemains = balanceAmount != null && balanceAmount <= feeAmount

        when (tossStatus) {

            TOSS_STATUS_CANCELED -> payment.markCanceled()
            TOSS_STATUS_PARTIAL_CANCELED -> if (onlyFeeRemains) payment.markCanceled() else payment.markPartialCanceled()
            /**
             * 이 시점은 토스 취소가 이미 끝난 뒤라 예외를 던지면 환불된 사용자가 실패 응답을 받는다.
             * 상태는 그대로 두고 로그로 남겨 수동으로 확인한다.
             */
            else -> log.error("예상하지 못한 토스 취소 상태입니다. paymentKey = {}, tossStatus = {}", paymentKey, tossStatus)
        }

        return payment
    }

    override fun markAbandoned(
        orderId: String,
        failureReason: String?
    ) {

        findByOrderId(orderId).markAbandoned(failureReason)
    }

    companion object {

        private const val TOSS_STATUS_CANCELED = "CANCELED"
        private const val TOSS_STATUS_PARTIAL_CANCELED = "PARTIAL_CANCELED"
    }
}