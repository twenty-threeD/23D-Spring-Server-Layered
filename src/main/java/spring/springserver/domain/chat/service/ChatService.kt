package spring.springserver.domain.chat.service

import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatParticipantResponse
import spring.springserver.domain.chat.data.response.ChatPaymentResponse
import spring.springserver.domain.chat.data.response.ChatRoomResponse
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse

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
        roomId: Long
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
}
