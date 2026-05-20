package spring.springserver.domain.email.service

import spring.springserver.domain.email.data.response.SendVerifyCodeResponse
import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse

interface EmailService {

    fun sendVerifyCode(email: String): SendVerifyCodeResponse

    fun checkVerifyCode(email: String,
                        code: String): CheckVerifyCodeResponse
}