package spring.springserver.domain.chat.data.response

enum class ChatMessageType {
    TEXT,
    PAYMENT,

    /**
     * 통화가 끝난 뒤 서버가 남기는 통화 기록.
     */
    CALL
}