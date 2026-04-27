package spring.springserver.domain.chat.service;

import spring.springserver.domain.chat.data.request.CreateChatRoomRequest;
import spring.springserver.domain.chat.data.request.SendChatMessageRequest;
import spring.springserver.domain.chat.data.response.ChatMessageResponse;
import spring.springserver.domain.chat.data.response.ChatRoomResponse;
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse;

import java.util.List;

public interface ChatService {

    CreateChatRoomResponse createOrGetDirectRoom(String requesterUsername,
                                                 CreateChatRoomRequest request);

    List<ChatRoomResponse> getMyChatRooms(String username);

    List<ChatMessageResponse> getRoomMessages(String username,
                                              Long roomId);

    ChatMessageResponse sendMessage(String senderUsername,
                                    SendChatMessageRequest request);

    boolean canAccessRoom(String username,
                          Long roomId);
}
