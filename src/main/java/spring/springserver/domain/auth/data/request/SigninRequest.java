package spring.springserver.domain.auth.data.request;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(

        @NotBlank
        String username,

        @NotBlank
        String password
) { }
