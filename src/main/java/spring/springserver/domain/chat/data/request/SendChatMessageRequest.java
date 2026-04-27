package spring.springserver.domain.chat.data.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendChatMessageRequest(

        @NotNull
        Long roomId,

        @NotBlank
        String message
) {
}
