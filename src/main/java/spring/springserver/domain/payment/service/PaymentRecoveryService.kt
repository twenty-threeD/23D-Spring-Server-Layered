package spring.springserver.domain.payment.service

interface PaymentRecoveryService {

    fun retryPendingCancels()

    fun reconcileStuckPayments()
}