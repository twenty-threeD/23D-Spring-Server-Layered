package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class SetPasswordRequest(
        @field:NotBlank
        @field:Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,32}$",
            message = "비밀번호는 8자 이상 32자 이하이어야 하며, 영문 대소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다."
        )
        @field:JsonProperty("new_password")
        val newPassword: String
)