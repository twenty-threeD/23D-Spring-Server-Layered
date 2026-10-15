package spring.springserver.domain.call.entity

enum class CallStatus {

    /**
     * 발신 후 수신자의 응답을 기다리는 상태.
     */
    RINGING,

    /**
     * 수신자가 받아 통화가 연결된 상태.
     */
    ACCEPTED,

    /**
     * 수신자가 거절한 상태.
     */
    REJECTED,

    /**
     * 발신자가 연결 전에 취소한 상태.
     */
    CANCELED,

    /**
     * 수신자가 제한 시간 안에 응답하지 않은 상태.
     */
    MISSED,

    /**
     * 연결된 통화가 종료된 상태.
     */
    ENDED;

    fun isFinished(): Boolean = this != RINGING && this != ACCEPTED
}
