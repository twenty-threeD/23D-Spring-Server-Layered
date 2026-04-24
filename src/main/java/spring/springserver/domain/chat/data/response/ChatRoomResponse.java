package spring.springserver.domain.chat.data.response;

import java.time.Instant;

public record ChatRoomResponse(
        Long roomId,
        String participantUsername,
        String participantName,
        String lastMessagePreview,
        Instant lastMessageAt
) {
}
