package spring.springserver.domain.oauth;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import spring.springserver.domain.member.entity.Role;

public record OAuthRequest (

    @NotBlank
    String username,

    @Email
    @NotBlank
    String email,

    @NotBlank
    String name,

    @Enumerated(EnumType.STRING)
    Role role
) {
}
