package spring.springserver.domain.chat.data.response

data class CreateChatRoomResponse(
    val roomId: Long?,
    val postId: Long?,
    val participantUsername: String,
    val existingRoom: Boolean
) {

    companion object {

        fun of(
            roomId: Long?,
            postId: Long?,
            participantUsername: String,
            existingRoom: Boolean
        ): CreateChatRoomResponse =
            CreateChatRoomResponse(
                roomId = roomId,
                postId = postId,
                participantUsername = participantUsername,
                existingRoom = existingRoom
            )
    }
}
