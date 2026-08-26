package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class ChangePhoneRequest(
    @field:NotBlank
    val password: String,

    @field:NotBlank
    @field:JsonProperty("new_phone")
    val newPhone: String,

    @field:NotBlank
    val code: String
)
