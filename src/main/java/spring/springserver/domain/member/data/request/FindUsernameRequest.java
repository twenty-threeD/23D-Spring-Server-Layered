package spring.springserver.domain.member.data.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindUsernameRequest(

        @Email
        @NotBlank
        String email
) {
}
