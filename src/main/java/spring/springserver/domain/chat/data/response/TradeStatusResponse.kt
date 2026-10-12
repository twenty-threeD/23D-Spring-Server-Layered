package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.entity.TradeStatus

data class TradeStatusResponse(
    val roomId: Long?,

    val tradeStatus: TradeStatus
) {

    companion object {

        fun of(
            room: ChatRoom
        ): TradeStatusResponse {

            return TradeStatusResponse(
                room.getId(),
                room.getTradeStatus()
            )
        }
    }
}