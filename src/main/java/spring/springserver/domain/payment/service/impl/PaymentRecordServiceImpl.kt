package spring.springserver.domain.payment.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.payment.data.request.PreparePaymentRequest
import spring.springserver.domain.payment.entity.Payment
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.payment.exception.PaymentStatusCode
import spring.springserver.domain.payment.repository.PaymentRepository
import spring.springserver.domain.payment.service.PaymentRecordService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class], propagation = Propagation.REQUIRES_NEW)
class PaymentRecordServiceImpl(
    private val paymentRepository: PaymentRepository
): PaymentRecordService {

    override fun create(
        preparePaymentRequest: PreparePaymentRequest,
        memberId: Long
    ): Payment {

        try {

            return paymentRepository.saveAndFlush(
                Payment(
                    preparePaymentRequest.orderId,
                    preparePaymentRequest.amount,
                    memberId,
                    preparePaymentRequest.contractUrl,
                    preparePaymentRequest.orderName,
                    preparePaymentRequest.roomId
                )
            )
        } catch (exception: DataIntegrityViolationException) {

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

    override fun markAbandoned(
        orderId: String,
        failureReason: String?
    ) {

        findByOrderId(orderId).markAbandoned(failureReason)
    }
}