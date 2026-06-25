package spring.springserver.domain.chat.data.response

data class CreateChatRoomResponse(
    val roomId: Long?,
    val participantUsername: String,
    val existingRoom: Boolean
) {

    companion object {

        fun of(
            roomId: Long?,
            participantUsername: String,
            existingRoom: Boolean
        ): CreateChatRoomResponse =
            CreateChatRoomResponse(
                roomId = roomId,
                participantUsername = participantUsername,
                existingRoom = existingRoom
            )
    }
}
