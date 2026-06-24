package spring.springserver.domain.chat.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.chat.data.request.CreateChatRoomRequest
import spring.springserver.domain.chat.data.request.SendChatMessageRequest
import spring.springserver.domain.chat.data.response.ChatMessageResponse
import spring.springserver.domain.chat.data.response.ChatRoomResponse
import spring.springserver.domain.chat.data.response.CreateChatRoomResponse
import spring.springserver.domain.chat.entity.ChatMessage
import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.chat.entity.ChatRoomParticipant
import spring.springserver.domain.chat.entity.MessageType
import spring.springserver.domain.chat.repository.ChatMessageRepository
import spring.springserver.domain.chat.repository.ChatRoomParticipantRepository
import spring.springserver.domain.chat.repository.ChatRoomRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.Instant

@Service
@Transactional
class ChatServiceImpl(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomParticipantRepository: ChatRoomParticipantRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
) : ChatService {

    override fun createOrGetDirectRoom(
        requesterUsername: String,
        createChatRoomRequest: CreateChatRoomRequest
    ): CreateChatRoomResponse {

        if (requesterUsername == createChatRoomRequest.username) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "자기 자신과는 채팅방을 만들 수 없습니다."
            )
        }

        val requester = getMemberByUsername(requesterUsername)
        val target = getMemberByUsername(createChatRoomRequest.username)
        val directChatKey = ChatRoom.generateDirectChatKey(requester.getId()!!, target.getId()!!)

        val existingRoom = chatRoomRepository.findByDirectChatKey(directChatKey)

        return if (existingRoom != null) {

            ensureParticipantRows(existingRoom)
            reactivateParticipant(existingRoom, requester)

            CreateChatRoomResponse.of(
                roomId = existingRoom.getId(),
                participantUsername = getOtherParticipant(existingRoom, requesterUsername).username,
                existingRoom = true
            )
        } else {

            createRoom(
                professional = requester,
                client = target
            )
        }
    }

    override fun getMyChatRooms(
        username: String
    ): List<ChatRoomResponse> {

        chatRoomRepository.findAllByParticipantUsername(username)
            .forEach(::ensureParticipantRows)

        return chatRoomParticipantRepository.findVisibleParticipantsByUsername(username)
            .map {

                participant ->
                val room = participant.room
                val other = getOtherParticipant(room, username)

                ChatRoomResponse.of(
                    room = room,
                    participant = other
                )
            }
    }

    override fun getRoomMessages(
        username: String,
        roomId: Long
    ): List<ChatMessageResponse> {

        val participant = getVisibleParticipant(
            roomId = roomId,
            username = username
        )

        val messages = if (participant.deletedAt == null) {

            chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId)
        } else {

            chatMessageRepository.findAllByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(
                roomId = roomId,
                createdAt = participant.deletedAt!!
            )
        }

        return messages.map(ChatMessageResponse::from)
    }

    override fun sendMessage(
        senderUsername: String,
        sendChatMessageRequest: SendChatMessageRequest
    ): ChatMessageResponse {

        val senderParticipant = getVisibleParticipant(
            roomId = sendChatMessageRequest.roomId,
            username = senderUsername
        )
        val room = senderParticipant.room
        val sender = senderParticipant.member
        val normalizedMessage = normalizeMessage(sendChatMessageRequest.message)
        val createdAt = Instant.now()

        val chatMessage = chatMessageRepository.save(
            ChatMessage(
                room = room,
                sender = sender,
                message = normalizedMessage,
                messageType = MessageType.TEXT,
                createdAt = createdAt
            )
        )

        room.updateLastMessageMeta(
            at = createdAt,
            preview = "새 메시지"
        )

        reactivateParticipantsOnNewMessage(
            room = room,
            senderId = sender.getId()
        )

        return ChatMessageResponse.of(chatMessage)
    }

    override fun canAccessRoom(
        username: String,
        roomId: Long
    ): Boolean {

        val room = chatRoomRepository.findByIdWithParticipants(roomId)
            ?: return false

        ensureParticipantRows(room)

        return chatRoomParticipantRepository.existsVisibleParticipant(
            roomId = roomId,
            username = username
        )
    }

    override fun leaveRoom(
        username: String,
        roomId: Long
    ) {

        val participant = getVisibleParticipant(roomId, username)
        participant.leave(Instant.now())
    }

    private fun createRoom(
        professional: Member,
        client: Member
    ): CreateChatRoomResponse {

        val room = if (isProfessional(professional)) {

            ChatRoom(client = client, professional = professional)
        } else {

            ChatRoom(client = professional, professional = client)
        }

        val savedRoom = chatRoomRepository.save(room)

        chatRoomParticipantRepository.save(ChatRoomParticipant(savedRoom, savedRoom.client))
        chatRoomParticipantRepository.save(ChatRoomParticipant(savedRoom, savedRoom.professional))

        return CreateChatRoomResponse.of(
            roomId = savedRoom.getId(),
            participantUsername = client.username,
            existingRoom = false
        )
    }

    private fun getVisibleParticipant(
        roomId: Long,
        username: String
    ): ChatRoomParticipant {

        val room = chatRoomRepository.findByIdWithParticipants(roomId)
            ?: throw ApplicationException.of(
                CommonStatusCode.ENDPOINT_NOT_FOUND,
                "존재하지 않는 채팅방입니다."
            )

        ensureParticipantRows(room)

        val participant = chatRoomParticipantRepository.findByRoomIdAndMemberUsername(roomId, username)
            ?: throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "해당 채팅방에 접근할 수 없습니다."
            )

        if (!participant.visible) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "나간 채팅방입니다."
            )
        }

        return participant
    }

    private fun getMemberByUsername(
        username: String
    ): Member =
        memberRepository.findByUsername(username)
            ?: throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "존재하지 않는 사용자입니다: $username"
            )

    private fun getOtherParticipant(
        room: ChatRoom,
        username: String
    ): Member {

        if (room.client.username == username) {

            return room.professional
        }

        if (room.professional.username == username) {

            return room.client
        }

        throw ApplicationException.of(
            CommonStatusCode.INVALID_ARGUMENT,
            "채팅방 참여자가 아닙니다."
        )
    }

    private fun isProfessional(
        member: Member
    ): Boolean =
        member.role.name == "PROFESSIONAL"

    private fun normalizeMessage(
        message: String
    ): String {

        val normalizedMessage = message.trim()

        if (normalizedMessage.length > 1000) {

            throw ApplicationException.of(
                CommonStatusCode.INVALID_ARGUMENT,
                "메시지는 1000자를 초과할 수 없습니다."
            )
        }

        return normalizedMessage
    }

    private fun ensureParticipantRows(
        room: ChatRoom
    ) {

        ensureParticipantRow(room, room.client)
        ensureParticipantRow(room, room.professional)
    }

    private fun ensureParticipantRow(
        room: ChatRoom,
        member: Member
    ) {

        if (chatRoomParticipantRepository.findByRoomIdAndMemberId(room.getId(), member.getId()) != null) {

            return
        }

        chatRoomParticipantRepository.save(
            ChatRoomParticipant(
                room = room,
                member = member
            )
        )
    }

    private fun reactivateParticipant(
        room: ChatRoom,
        member: Member
    ) {

        val participant = chatRoomParticipantRepository.findByRoomIdAndMemberId(
            roomId = room.getId(),
            memberId = member.getId()
        ) ?: throw ApplicationException.of(
            CommonStatusCode.ENDPOINT_NOT_FOUND,
            "채팅방 참여자 정보가 없습니다."
        )

        participant.reactivate()
    }

    private fun reactivateParticipantsOnNewMessage(
        room: ChatRoom,
        senderId: Long?
    ) {

        val participants = chatRoomParticipantRepository.findAllByRoomId(room.getId()!!)

        for (participant in participants) {

            if (participant.member.getId() == senderId) {

                continue
            }

            participant.reactivate()
        }
    }
}
