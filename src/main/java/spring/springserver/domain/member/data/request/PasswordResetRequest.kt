package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
        @field:NotBlank
        val username: String,

        @field:NotBlank
        @field:JsonProperty("new_password")
        val newPassword: String
)
