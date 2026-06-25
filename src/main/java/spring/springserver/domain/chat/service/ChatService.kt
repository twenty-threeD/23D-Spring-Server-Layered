package spring.springserver.domain.chat.service

import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
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

    fun canAccessRoom(
        username: String,
        roomId: Long
    ): Boolean

    fun leaveRoom(
        username: String,
        roomId: Long
    )
}
