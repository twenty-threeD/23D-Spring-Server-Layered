package spring.springserver.domain.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest;
import spring.springserver.domain.chat.data.request.SendChatMessageRequest;
import spring.springserver.domain.chat.data.response.ChatMessageResponse;
import spring.springserver.domain.chat.data.response.ChatRoomResponse;
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse;
import spring.springserver.domain.chat.entity.ChatMessage;
import spring.springserver.domain.chat.entity.MessageType;
import spring.springserver.domain.chat.entity.ChatRoom;
import spring.springserver.domain.chat.entity.ChatRoomParticipant;
import spring.springserver.domain.chat.repository.ChatMessageRepository;
import spring.springserver.domain.chat.repository.ChatRoomParticipantRepository;
import spring.springserver.domain.chat.repository.ChatRoomRepository;
import spring.springserver.domain.member.entity.Member;
import spring.springserver.domain.member.repository.MemberRepository;
import spring.springserver.global.exception.exception.ApplicationException;
import spring.springserver.global.exception.status_code.CommonStatusCode;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    private static final String LAST_MESSAGE_PLACEHOLDER = "새 메시지";

    @Override
    @Transactional
    public CreateChatRoomResponse createOrGetDirectRoom(String username,
                                                        CreateChatRoomRequest createChatRoomRequest) {

        if (username.equals(createChatRoomRequest.username())) {
            
            throw ApplicationException.of(
                    CommonStatusCode.INVALID_ARGUMENT,
                    "자기 자신과는 채팅방을 만들 수 없습니다."
            );
        }

        Member requester = getMemberByUsername(username);
        Member target = getMemberByUsername(createChatRoomRequest.username());
        String directChatKey = ChatRoom.generateDirectChatKey(requester.getId(), target.getId());

        return chatRoomRepository.findByDirectChatKey(directChatKey)
                .map(room -> {
                    ensureParticipantRows(room);
                    reactivateParticipant(room, requester);

                    return new CreateChatRoomResponse(
                            room.getId(),
                            getOtherParticipant(room, username).getUsername(),
                            true
                    );
                }).orElseGet(
                        () -> createRoom(
                                requester,
                                target
                        )
                );
    }

    @Override
    public List<ChatRoomResponse> getMyChatRooms(String username) {

        chatRoomRepository.findAllByParticipantUsername(username)
                .forEach(this::ensureParticipantRows);

        return chatRoomParticipantRepository.findVisibleParticipantsByUsername(username)
                .stream()
                .map(participant -> {
                    ChatRoom room = participant.getRoom();
                    Member other = getOtherParticipant(room, username);

                    return new ChatRoomResponse(
                            room.getId(),
                            other.getUsername(),
                            other.getName(),
                            room.getLastMessagePreview(),
                            room.getLastMessageAt()
                    );
                }).toList();
    }

    @Override
    public List<ChatMessageResponse> getRoomMessages(String username,
                                                     Long roomId) {

        ChatRoomParticipant participant = getVisibleParticipant(
                roomId,
                username
        );

        List<ChatMessage> messages = participant.getDeletedAt() == null
                ? chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId)
                : chatMessageRepository.findAllByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        roomId,
                        participant.getDeletedAt()
                );

        return messages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(String username,
                                           SendChatMessageRequest sendChatMessageRequest) {

        ChatRoomParticipant senderParticipant = getVisibleParticipant(
                sendChatMessageRequest.roomId(),
                username
        );

        ChatRoom room = senderParticipant.getRoom();

        Member sender = senderParticipant.getMember();

        String normalizedMessage = normalizeMessage(sendChatMessageRequest.message());

        Instant createdAt = Instant.now();

        ChatMessage chatMessage = chatMessageRepository.save(
                new ChatMessage(
                        null,
                        room,
                        sender,
                        normalizedMessage,
                        MessageType.TEXT,
                        createdAt
                )
        );

        room.updateLastMessageMeta(
                createdAt,
                LAST_MESSAGE_PLACEHOLDER
        );

        reactivateParticipantsOnNewMessage(
                room,
                sender.getId()
        );

        return ChatMessageResponse.of(chatMessage);
    }

    @Override
    public boolean canAccessRoom(String username,
                                 Long roomId) {

        ChatRoom room = chatRoomRepository.findByIdWithParticipants(roomId)
                .orElse(null);

        if (room == null) {

            return false;
        }

        ensureParticipantRows(room);

        return chatRoomParticipantRepository.existsVisibleParticipant(
                roomId,
                username
        );
    }

    @Override
    @Transactional
    public void leaveRoom(String username,
                          Long roomId) {

        ChatRoomParticipant participant = getVisibleParticipant(roomId, username);

        participant.leave(Instant.now());
    }

    private CreateChatRoomResponse createRoom(Member professional,
                                              Member client) {

        ChatRoom room = isProfessional(professional)
                ? new ChatRoom(client, professional)
                : new ChatRoom(professional, client);

        ChatRoom savedRoom = chatRoomRepository.save(room);

        chatRoomParticipantRepository.save(new ChatRoomParticipant(savedRoom, savedRoom.getClient()));
        chatRoomParticipantRepository.save(new ChatRoomParticipant(savedRoom, savedRoom.getProfessional()));

        return new CreateChatRoomResponse(
                savedRoom.getId(),
                client.getUsername(),
                false
        );
    }

    private ChatRoomParticipant getVisibleParticipant(Long roomId,
                                                      String username) {

        ChatRoom room = chatRoomRepository.findByIdWithParticipants(roomId)
                .orElseThrow(
                        () -> ApplicationException.of(
                                CommonStatusCode.ENDPOINT_NOT_FOUND,
                                "존재하지 않는 채팅방입니다."
                        )
                );
        ensureParticipantRows(room);

        ChatRoomParticipant participant = chatRoomParticipantRepository.findByRoomIdAndMemberUsername(roomId, username)
                .orElseThrow(
                        () -> ApplicationException.of(
                                CommonStatusCode.INVALID_ARGUMENT,
                                "해당 채팅방에 접근할 수 없습니다."
                        )
                );

        if (!participant.isVisible()) {

            throw ApplicationException.of(
                    CommonStatusCode.INVALID_ARGUMENT,
                    "나간 채팅방입니다."
            );
        }

        return participant;
    }

    private Member getMemberByUsername(String username) {

        return Optional.ofNullable(memberRepository.findByUsername(username))
                .orElseThrow(
                        () -> ApplicationException.of(
                                CommonStatusCode.INVALID_ARGUMENT,
                                "존재하지 않는 사용자입니다: " + username
                        )
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

        throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "채팅방 참여자가 아닙니다."
        );
    }

    private boolean isProfessional(Member member) {

        return member.getRole().name().equals("PROFESSIONAL");
    }

    private String normalizeMessage(String message) {

        String normalizedMessage = message.trim();

        if (normalizedMessage.length() > 1000) {

            throw ApplicationException.of(
                    CommonStatusCode.INVALID_ARGUMENT,
                    "메시지는 1000자를 초과할 수 없습니다."
            );
        }

        return normalizedMessage;
    }

    private void ensureParticipantRows(ChatRoom room) {

        ensureParticipantRow(room, room.getClient());
        ensureParticipantRow(room, room.getProfessional());
    }

    private void ensureParticipantRow(ChatRoom room,
                                      Member member) {

        if (chatRoomParticipantRepository.findByRoomIdAndMemberId(room.getId(), member.getId()).isPresent()) {

            return;
        }

        chatRoomParticipantRepository.save(new ChatRoomParticipant(
                room,
                member
        ));
    }

    private void reactivateParticipant(ChatRoom room,
                                       Member member) {

        ChatRoomParticipant participant = chatRoomParticipantRepository.findByRoomIdAndMemberId(
                room.getId(),
                member.getId()
                )

                .orElseThrow(
                        () -> ApplicationException.of(
                                CommonStatusCode.ENDPOINT_NOT_FOUND,
                                "채팅방 참여자 정보가 없습니다."
                        )
                );
        participant.reactivate();
    }

    private void reactivateParticipantsOnNewMessage(ChatRoom room,
                                                    Long senderId) {

        List<ChatRoomParticipant> participants = chatRoomParticipantRepository.findAllByRoomId(room.getId());

        for (ChatRoomParticipant participant : participants) {

            if (Objects.requireNonNull(participant.getMember().getId()).equals(senderId)) {

                continue;
            }

            participant.reactivate();
        }
    }
}
