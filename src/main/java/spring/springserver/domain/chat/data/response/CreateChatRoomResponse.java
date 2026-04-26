package spring.springserver.domain.chat.data.response;

public record CreateChatRoomResponse(

        Long roomId,
        String participantUsername,
        boolean existingRoom
) {
}
