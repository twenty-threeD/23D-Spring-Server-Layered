package spring.springserver.domain.member.data.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeUsernameRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @JsonProperty(value = "new_username")
        String newUsername
) {
}