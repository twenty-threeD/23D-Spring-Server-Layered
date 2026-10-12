package spring.springserver.domain.chat.service

import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.data.request.UpdateTradeStatusRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatParticipantResponse
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.data.response.ChatRoomResponse
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse
import spring.springserver.domain.chat.data.response.TradeStatusResponse

interface ChatService {

    fun createOrGetDirectRoom(
        requesterUsername: String,
        createChatRoomRequest: CreateChatRoomRequest
    ): CreateChatRoomResponse

    fun getMyChatRooms(
        username: String
    ): List<ChatRoomResponse>

    fun getRoomMessages(
        username: String,
        roomId: Long,
        cursor: Long?,
        after: Long?,
        size: Int
    ): List<ChatMessageResponse>

    fun sendMessage(
        senderUsername: String,
        sendChatMessageRequest: SendChatMessageRequest
    ): ChatMessageResponse

    fun getOtherParticipantUsername(
        roomId: Long,
        username: String
    ): ChatParticipantResponse

    fun canAccessRoom(
        username: String,
        roomId: Long
    ): Boolean

    fun leaveRoom(
        username: String,
        roomId: Long
    )

    fun findDirectRoomId(
        clientId: Long,
        professionalId: Long,
        postId: Long
    ): Long?

    fun isRoomParticipant(
        roomId: Long,
        memberId: Long
    ): Boolean

    fun sendPaymentMessage(
        roomId: Long,
        senderMemberId: Long,
        payment: ChatPaymentResponse
    ): ChatMessageResponse

    /**
     * 방 참여자가 진행 중인 거래를 직접 취소한다.
     */
    fun updateTradeStatus(
        username: String,
        roomId: Long,
        updateTradeStatusRequest: UpdateTradeStatusRequest
    ): TradeStatusResponse

    /**
     * 결제 승인이 체인 기록까지 끝나면 결제 도메인에서 호출한다.
     */
    fun completeTrade(
        roomId: Long
    )

    /**
     * 결제가 전액 취소되면 결제 도메인에서 호출한다.
     */
    fun cancelTrade(
        roomId: Long
    )

    /**
     * 같은 방에서 새 계약서가 등록되면 계약 도메인에서 호출해 거래를 다시 진행 중으로 돌린다.
     * 해당하는 방이 없으면 아무것도 하지 않는다.
     */
    fun restartTrade(
        clientId: Long,
        professionalId: Long,
        postId: Long
    )
}
