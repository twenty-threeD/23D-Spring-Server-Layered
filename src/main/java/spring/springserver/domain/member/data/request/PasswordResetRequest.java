package spring.springserver.domain.member.data.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

        @NotBlank
        String username,

        @NotBlank
        String newPassword
) {
}
