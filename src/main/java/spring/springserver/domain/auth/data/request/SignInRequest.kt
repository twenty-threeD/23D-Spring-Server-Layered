package spring.springserver.domain.auth.data.request

import jakarta.validation.constraints.NotBlank

data class SignInRequest(
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)
