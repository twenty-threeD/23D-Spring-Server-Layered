package spring.springserver.domain.blockchain.data.response

/**
 * 검증이 실패한 이유를 구분한다.
 * 검증 실패는 서버 오류가 아니라 정상적인 검증 결과이므로 예외로 던지지 않고 응답에 담는다.
 */
enum class VerificationFailureReason {

    INVALID_TX_HASH,
    TX_NOT_FOUND,
    TX_FAILED,
    NOT_PAYMENT_RECORD,
    NOT_ON_LEDGER,
    LEDGER_MISMATCH,
    SIGNATURE_INVALID,
    SIGNATURE_UNVERIFIABLE
}