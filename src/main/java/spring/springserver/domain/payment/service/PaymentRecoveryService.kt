package spring.springserver.domain.payment.service

interface PaymentRecoveryService {

    fun retryPendingCancels()

    fun reconcileStuckPayments()

    fun reconcileUnsyncedCancels()

    /**
     * 체인 커밋 결과가 미확정인 결제를 원장 조회로 확정한다.
     * 기록이 있으면 완료로, 유예 한도까지 없으면 보상 취소 대상으로 넘긴다.
     */
    fun reconcileUnconfirmedChainRecords()
}