package spring.springserver.domain.auth.data.request;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import spring.springserver.domain.member.entity.Role;

public record GenerateTokenRequest(

        @NotBlank
        String username,

        @NotNull
        @Enumerated(EnumType.STRING)
        Role role
) {
}