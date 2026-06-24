package spring.springserver.domain.email.data.request

import jakarta.validation.constraints.Email

data class CheckVerifyCodeRequest(

    @field:Email
    val email: String,

    val verifyCode: String
)
