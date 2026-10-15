package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PasswordChangeRequest(
        @field:NotBlank
        @field:JsonProperty("current_password")
        val currentPassword: String,

        @field:NotBlank
        @field:JsonProperty("new_password")
        val newPassword: String
)
