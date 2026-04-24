package spring.springserver.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest;
import spring.springserver.domain.chat.data.request.SendChatMessageRequest;
import spring.springserver.domain.chat.data.response.ChatMessageResponse;
import spring.springserver.domain.chat.data.response.ChatRoomResponse;
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse;
import spring.springserver.domain.chat.entity.ChatMessage;
import spring.springserver.domain.chat.entity.ChatRoom;
import spring.springserver.domain.chat.repository.ChatMessageRepository;
import spring.springserver.domain.chat.repository.ChatRoomRepository;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;
import spring.springserver.global.exception.status_code.CommonStatusCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public CreateChatRoomResponse createOrGetDirectRoom(String username,
                                                        CreateChatRoomRequest createChatRoomRequest) {

        if (username.equals(createChatRoomRequest.username())) {

            throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT,
                            "자기 자신과는 채팅방을 만들 수 없습니다.");
        }

        Member requester = getMemberByUsername(username);
        Member target = getMemberByUsername(createChatRoomRequest.username());
        String directChatKey = ChatRoom.generateDirectChatKey(requester.getId(), target.getId());

        return chatRoomRepository.findByDirectChatKey(directChatKey)
                .map(room -> new CreateChatRoomResponse(
                        room.getId(),
                        getOtherParticipant(room, username).getUsername(),
                        true
                ))
                .orElseGet(() -> createRoom(requester, target));
    }

    @Override
    public List<ChatRoomResponse> getMyChatRooms(String username) {

        return chatRoomRepository.findAllByParticipantUsername(username)
                .stream()
                .map(room -> {
                    Member other = getOtherParticipant(room, username);

                    return new ChatRoomResponse(
                            room.getId(),
                            other.getUsername(),
                            other.getName(),
                            room.getLastMessagePreview(),
                            room.getLastMessageAt()
                    );
                })
                .toList();
    }

    @Override
    public List<ChatMessageResponse> getRoomMessages(String username,
                                                     Long roomId) {

        ChatRoom room = getAuthorizedRoom(roomId, username);

        return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(message -> ChatMessageResponse.from(message, room.getId()))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(String username,
                                           SendChatMessageRequest sendChatMessageRequest) {

        ChatRoom room = getAuthorizedRoom(sendChatMessageRequest.roomId(), username);
        Member sender = getParticipant(room, username);
        String normalizedMessage = normalizeMessage(sendChatMessageRequest.message());

        ChatMessage chatMessage = chatMessageRepository.save(new ChatMessage(normalizedMessage, room, sender));
        room.updateLastMessageMeta(chatMessage.getCreatedAt(), normalizedMessage);

        return ChatMessageResponse.from(chatMessage, room.getId());
    }

    private CreateChatRoomResponse createRoom(Member professional,
                                              Member client) {

        ChatRoom room = isProfessional(professional)
                ? new ChatRoom(client, professional)
                : new ChatRoom(professional, client);

        ChatRoom savedRoom = chatRoomRepository.save(room);

        return new CreateChatRoomResponse(
                savedRoom.getId(),
                client.getUsername(),
                false
        );
    }

    private ChatRoom getAuthorizedRoom(Long roomId,
                                       String username) {

        ChatRoom room = chatRoomRepository.findByIdWithParticipants(roomId)
                .orElseThrow(
                        () -> ApplicationException.of(CommonStatusCode.ENDPOINT_NOT_FOUND, "존재하지 않는 채팅방입니다."));

        if (!isParticipant(room, username)) {

            throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT,
                            "해당 채팅방에 접근할 수 없습니다.");
        }

        return room;
    }

    private Member getMemberByUsername(String username) {

        return memberRepository.findByUsername(username)
                .orElseThrow(
                        () -> ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT,
                                        "존재하지 않는 사용자입니다: " + username)
                );
    }

    private Member getOtherParticipant(ChatRoom room,
                                       String username) {

        if (room.getClient().getUsername().equals(username)) {

            return room.getProfessional();
        }

        if (room.getProfessional().getUsername().equals(username)) {

            return room.getClient();
        }

        throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT, "채팅방 참여자가 아닙니다.");
    }

    private Member getParticipant(ChatRoom room,
                                  String username) {

        if (room.getClient().getUsername().equals(username)) {

            return room.getClient();
        }

        if (room.getProfessional().getUsername().equals(username)) {

            return room.getProfessional();
        }

        throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT, "채팅방 참여자가 아닙니다.");
    }

    //참여 확인 메서드
    private boolean isParticipant(ChatRoom room,
                                  String username) {

        return room.getClient().getUsername().equals(username)
                || room.getProfessional().getUsername().equals(username);
    }

    //professional 구분 메서드
    private boolean isProfessional(Member member) {

        return member.getRole() != null && member.getRole().name().equals("PROFESSIONAL");
    }

    //길이 제한 메서드
    private String normalizeMessage(String message) {

        String normalizedMessage = message.trim();

        if (normalizedMessage.length() > 1000) {

            throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT,
                            "메시지는 1000자를 초과할 수 없습니다.");
        }

        return normalizedMessage;
    }
}
