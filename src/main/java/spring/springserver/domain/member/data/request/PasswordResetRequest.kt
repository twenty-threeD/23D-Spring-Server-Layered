package spring.springserver.domain.member.data.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
        @field:NotBlank
        val username: String,

        @field:NotBlank
        @field:JsonProperty("new_password")
        val newPassword: String,

        /**
         * 본인 확인 수단. 휴대폰(phone) 또는 이메일(email) 중 하나를 코드와 함께 보낸다.
         * 둘 다 없거나 코드가 없으면 재설정을 거부한다.
         */
        val phone: String? = null,

        val email: String? = null,

        @field:JsonProperty("verify_code")
        val verifyCode: String? = null
)
