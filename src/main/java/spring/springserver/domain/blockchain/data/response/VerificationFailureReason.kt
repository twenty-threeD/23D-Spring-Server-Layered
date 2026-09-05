package spring.springserver.domain.blockchain.data.response

/**
 * 검증이 실패한 이유를 구분한다.
 * 검증 실패는 서버 오류가 아니라 정상적인 검증 결과이므로 예외로 던지지 않고 응답에 담는다.
 */
enum class VerificationFailureReason(
    val message: String
) {

    INVALID_TX_HASH("잘못된 트랜잭션 해시"),
    TX_NOT_FOUND("트랜잭션을 찾을 수 없습니다."),
    TX_FAILED("실패한 트랜잭션"),
    NOT_PAYMENT_RECORD("기록되지 않은 결제"),
    NOT_ON_LEDGER("결제 누락"),
    LEDGER_MISMATCH("결제 불일치"),
    SIGNATURE_INVALID("잘못된 서명"),
    SIGNATURE_UNVERIFIABLE("검증할 수 없는 서명")
}