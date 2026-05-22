package spring.springserver.domain.member.data.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class FindUsernameRequest(
        @field:Email
        @field:NotBlank
        val email: String
)