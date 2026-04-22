package spring.springserver.domain.member.data.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

        @NotBlank
        String username,

        @NotBlank
        @JsonProperty(value = "new_password")
        String newPassword
) {
}
