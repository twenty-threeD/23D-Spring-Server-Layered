package spring.springserver.domain.push.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterPushTokenRequest(
    @field:NotBlank
    @field:Size(max = 255)
    @field:Pattern(regexp = "^Expo(nent)?PushToken\\[.+]$", message = "Expo 푸시 토큰 형식이 아닙니다.")
    val token: String,

    @field:NotBlank
    @field:Pattern(regexp = "^(?i)(android|ios)$", message = "platform은 android 또는 ios여야 합니다.")
    val platform: String
)
