package spring.springserver.domain.phone.data.request

import jakarta.validation.constraints.NotBlank

data class SendPhoneVerifyNumberRequest(
    @field:NotBlank
    val recipientNumber: String
)
