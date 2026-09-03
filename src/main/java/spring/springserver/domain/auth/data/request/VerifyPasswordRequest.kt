package spring.springserver.domain.auth.data.request

import jakarta.validation.constraints.NotBlank

data class VerifyPasswordRequest(

    @field:NotBlank
    val password: String
)
