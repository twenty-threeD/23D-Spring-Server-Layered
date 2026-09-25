package spring.springserver.domain.auth.data.request

import jakarta.validation.constraints.NotBlank

data class OAuthExchangeRequest(
    @field:NotBlank
    val code: String
)