package spring.springserver.domain.chat.data.request;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRoomRequest(

        @NotBlank
        String username
) {
}
