package spring.springserver.domain.chat.data.response

import spring.springserver.domain.chat.entity.ChatRoom
import spring.springserver.domain.member.entity.Member
import java.time.Instant

data class ChatRoomResponse(
    val roomId: Long?,
    val postId: Long?,
    /**
     * 대화 상대의 memberId. 견적서 발행처럼 상대를 특정해야 하는 요청에 쓴다.
     */
    val participantId: Long?,
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
                participantId = participant.getId(),
                participantUsername = participant.username,
                participantName = participant.name,
                participantImageUrl = participantImageUrl,
                lastMessagePreview = room.lastMessagePreview,
                lastMessageAt = room.lastMessageAt,
                createdAt = room.getCreatedAt()
            )
    }
}
