package spring.springserver.domain.blockchain.data.response

enum class VerificationFailureReason(
    val message: String
) {

    INVALID_TX_HASH("잘못된 트랜잭션 해시"),
    TX_NOT_FOUND("트랜잭션을 찾을 수 없음"),
    TX_FAILED("실패한 트랜잭션"),
    NOT_PAYMENT_RECORD("기록되지 않은 결제"),
    NOT_ON_LEDGER("결제가 누락됨"),
    LEDGER_MISMATCH("결제가 일치하지 않음"),
    SIGNATURE_INVALID("잘못된 서명"),
    SIGNATURE_UNVERIFIABLE("검증할 수 없는 서명")
}