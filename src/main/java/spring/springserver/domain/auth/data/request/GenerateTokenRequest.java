package spring.springserver.domain.auth.data.request;

import jakarta.validation.constraints.NotBlank;
import spring.springserver.domain.member.entity.Role;

public record GenerateTokenRequest(

        @NotBlank
        String username,

        @NotBlank
        Role role
) { }
