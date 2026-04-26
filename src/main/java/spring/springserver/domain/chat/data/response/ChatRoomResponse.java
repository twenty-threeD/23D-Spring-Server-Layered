package spring.springserver.domain.chat.data.response;

import spring.springserver.domain.chat.entity.ChatRoom;
import spring.springserver.domain.member.entity.Member;

import java.time.Instant;

public record ChatRoomResponse(

        Long roomId,
        String participantUsername,
        String participantName,
        String lastMessagePreview,
        Instant lastMessageAt
) {

    public static ChatRoomResponse from(ChatRoom room, Member member) {

        return new ChatRoomResponse(
                room.getId(),
                member.getUsername(),
                member.getName(),
                room.getLastMessagePreview(),
                room.getLastMessageAt()
        );

    }
}
