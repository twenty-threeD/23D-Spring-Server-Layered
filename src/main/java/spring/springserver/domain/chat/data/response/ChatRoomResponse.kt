package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.member.entity.Member
import java.time.Instant

data class ChatRoomResponse(
    val roomId: Long?,
    val postId: Long?,
    val participantUsername: String,
    val participantName: String,
    val participantImageUrl: String?,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
    val createdAt: Instant?
) {

    companion object {

        fun of(
            room: ChatRoom,
            participant: Member,
            participantImageUrl: String?
        ): ChatRoomResponse =
            ChatRoomResponse(
                roomId = room.getId(),
                postId = room.post.getId(),
                participantUsername = participant.username,
                participantName = participant.name,
                participantImageUrl = participantImageUrl,
                lastMessagePreview = room.lastMessagePreview,
                lastMessageAt = room.lastMessageAt,
                createdAt = room.getCreatedAt()
            )
    }
}
