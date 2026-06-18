package spring.springserver.domain.email.data.request

import jakarta.validation.constraints.Email

data class SendVerifyCodeRequest(

    @field:Email
    val email: String
)