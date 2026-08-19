package spring.springserver.domain.payment.entity

enum class PaymentStatus {

    READY,
    IN_PROGRESS,
    DONE,
    CHAIN_RECORDED,
    CANCELED_BY_CHAIN_FAILURE,
    CANCEL_PENDING,
    CANCEL_FAILED,
    ABANDONED
}