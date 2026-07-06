package spring.springserver.domain.auth.data.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignInRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)
