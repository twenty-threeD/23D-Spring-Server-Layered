package spring.springserver.domain.auth.data.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(

        @Email
        @NotBlank
        String email
) {
}
