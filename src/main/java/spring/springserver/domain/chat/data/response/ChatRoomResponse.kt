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
    val createdAt: Instant?,
    /**
     * 이 시각 이전 메시지는 조회자에게 보이지 않는다.
     * 클라이언트는 로컬 DB에 남아 있는 이 시각 이전 메시지를 정리해야 한다.
     * 나간 적이 없으면 null이다.
     */
    val clearBefore: Instant?
) {

    companion object {

        fun of(
            room: ChatRoom,
            participant: Member,
            participantImageUrl: String?,
            clearBefore: Instant?
        ): ChatRoomResponse {

            /**
             * 마지막 메시지 정보는 방 단위 값이라 참여자별 워터마크가 적용돼 있지 않다.
             * 워터마크 이전 메시지의 흔적이 목록에 남지 않도록 여기서 가린다.
             */
            val hidden = clearBefore != null
                && room.lastMessageAt != null
                && !room.lastMessageAt!!.isAfter(clearBefore)

            return ChatRoomResponse(
                roomId = room.getId(),
                postId = room.post.getId(),
                participantId = participant.getId(),
                participantUsername = participant.username,
                participantName = participant.name,
                participantImageUrl = participantImageUrl,
                lastMessagePreview = if (hidden) null else room.lastMessagePreview,
                lastMessageAt = if (hidden) null else room.lastMessageAt,
                createdAt = room.getCreatedAt(),
                clearBefore = clearBefore
            )
        }
    }
}
