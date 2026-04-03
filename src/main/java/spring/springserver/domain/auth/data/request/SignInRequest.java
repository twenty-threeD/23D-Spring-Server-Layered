package spring.springserver.domain.auth.data.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(

        @NotBlank
        String username,

        @NotBlank
        String password
) { }
