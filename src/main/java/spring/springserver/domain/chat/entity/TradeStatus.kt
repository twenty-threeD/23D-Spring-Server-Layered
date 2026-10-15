package spring.springserver.domain.chat.entity

/**
 * 채팅방에서 오가는 거래의 상태.
 * 거래가 끝나도 채팅은 계속할 수 있으므로 메시지 전송과는 무관하다.
 */
enum class TradeStatus {

    IN_PROGRESS,
    COMPLETED,
    CANCELED
}