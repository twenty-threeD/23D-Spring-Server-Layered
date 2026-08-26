package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequest(
    @field:NotBlank
    val password: String,

    @field:Email
    @field:NotBlank
    @field:JsonProperty("new_email")
    val newEmail: String,

    @field:NotBlank
    @field:JsonProperty("verify_code")
    val verifyCode: String
)
