package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.member.entity.Member
import java.time.Instant

data class ChatRoomResponse(
    val roomId: Long?,
    val participantUsername: String,
    val participantName: String,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?
) {

    companion object {

        fun of(
            room: ChatRoom,
            participant: Member
        ): ChatRoomResponse =
            ChatRoomResponse(
                roomId = room.getId(),
                participantUsername = participant.username,
                participantName = participant.name,
                lastMessagePreview = room.lastMessagePreview,
                lastMessageAt = room.lastMessageAt
            )
    }
}
