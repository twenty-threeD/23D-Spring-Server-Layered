package spring.springserver.domain.chat.data.response

import java.time.Instant

data class CreateChatRoomResponse(
    val roomId: Long?,
    val postId: Long?,
    val participantUsername: String,
    val existingRoom: Boolean,
    /**
     * 이 시각 이전 메시지는 요청자에게 보이지 않는다.
     * 클라이언트는 로컬 DB에 남아 있는 이 시각 이전 메시지를 정리해야 한다.
     * 나간 적이 없으면 null이다.
     */
    val clearBefore: Instant?
) {

    companion object {

        fun of(
            roomId: Long?,
            postId: Long?,
            participantUsername: String,
            existingRoom: Boolean,
            clearBefore: Instant?
        ): CreateChatRoomResponse =
            CreateChatRoomResponse(
                roomId = roomId,
                postId = postId,
                participantUsername = participantUsername,
                existingRoom = existingRoom,
                clearBefore = clearBefore
            )
    }
}
