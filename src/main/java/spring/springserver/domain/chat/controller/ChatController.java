package spring.springserver.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest;
import spring.springserver.domain.chat.data.response.ChatMessageResponse;
import spring.springserver.domain.chat.data.response.ChatRoomResponse;
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse;
import spring.springserver.domain.chat.service.ChatService;
import spring.springserver.global.data.BaseResponse;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/rooms")
    public BaseResponse<CreateChatRoomResponse> createRoom(@RequestBody @Valid CreateChatRoomRequest createChatRoomRequest,
                                                           Principal principal) {

        return BaseResponse.ok(chatService.createOrGetDirectRoom(principal.getName(), createChatRoomRequest));
    }

    @GetMapping("/rooms")
    public BaseResponse<List<ChatRoomResponse>> getRooms(Principal principal) {

        return BaseResponse.ok(chatService.getMyChatRooms(principal.getName()));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public BaseResponse<List<ChatMessageResponse>> getMessages(@PathVariable Long roomId,
                                                               Principal principal) {

        return BaseResponse.ok(chatService.getRoomMessages(principal.getName(), roomId));
    }
}
