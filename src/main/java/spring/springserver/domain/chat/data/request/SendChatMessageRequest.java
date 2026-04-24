package spring.springserver.domain.chat.data.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendChatMessageRequest(
        @NotNull(message = "roomId는 필수입니다.")
        Long roomId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String message
) {
}
