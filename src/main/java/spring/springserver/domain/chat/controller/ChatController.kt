package spring.springserver.domain.chat.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatRoomResponse
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.global.data.BaseResponse
import java.security.Principal

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/rooms")
    fun createRoom(
        @RequestBody @Valid createChatRoomRequest: CreateChatRoomRequest,
        principal: Principal
    ): BaseResponse<CreateChatRoomResponse> =
        BaseResponse.ok(
            chatService.createOrGetDirectRoom(
                requesterUsername = principal.name,
                createChatRoomRequest = createChatRoomRequest
            )
        )

    @GetMapping("/rooms")
    fun getRooms(
        principal: Principal
    ): BaseResponse<List<ChatRoomResponse>> =
        BaseResponse.ok(
            chatService.getMyChatRooms(principal.name)
        )

    @GetMapping("/rooms/{roomId}/messages")
    fun getMessages(
        @PathVariable roomId: Long,
        principal: Principal
    ): BaseResponse<List<ChatMessageResponse>> =
        BaseResponse.ok(
            chatService.getRoomMessages(
                username = principal.name,
                roomId = roomId
            )
        )

    @DeleteMapping("/rooms/{roomId}")
    fun leaveRoom(
        @PathVariable roomId: Long,
        principal: Principal
    ): BaseResponse<Void> {

        chatService.leaveRoom(
            username = principal.name,
            roomId = roomId
        )

        return BaseResponse.ok(null)
    }
}
