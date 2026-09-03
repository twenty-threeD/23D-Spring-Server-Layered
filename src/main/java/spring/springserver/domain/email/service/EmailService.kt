package spring.springserver.domain.email.service

import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse
import spring.springserver.domain.email.data.response.SendVerifyCodeResponse

interface EmailService {

    fun sendVerifyCode(email: String): SendVerifyCodeResponse

    fun checkVerifyCode(email: String,
                        code: String): CheckVerifyCodeResponse

    fun sendChangeEmailCode(email: String): SendVerifyCodeResponse

    fun checkChangeEmailCode(email: String,
                             code: String): CheckVerifyCodeResponse
}