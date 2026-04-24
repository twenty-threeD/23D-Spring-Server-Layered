package spring.springserver.domain.chat.data.request;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRoomRequest(
        @NotBlank(message = "상대 username은 필수입니다.")
        String username
) {
}
