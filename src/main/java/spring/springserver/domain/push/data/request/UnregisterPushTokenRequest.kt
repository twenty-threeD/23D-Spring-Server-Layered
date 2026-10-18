package spring.springserver.domain.push.data.request

import jakarta.validation.constraints.NotBlank

data class UnregisterPushTokenRequest(
    @field:NotBlank
    val token: String
)
