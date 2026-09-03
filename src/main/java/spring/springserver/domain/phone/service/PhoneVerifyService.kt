package spring.springserver.domain.phone.service

import spring.springserver.domain.phone.data.response.PhoneVerifyResponse
import spring.springserver.domain.phone.data.response.SendPhoneVerifyNumberResponse

interface PhoneVerifyService {

    fun sendMessage(
        recipientNumber: String
    ): SendPhoneVerifyNumberResponse

    fun verifyPhone(
        recipientNumber: String,
        code: String
    ): PhoneVerifyResponse

    fun consumePhoneVerification(
        phone: String
    ): Boolean

    fun verifyCodeOnly(
        recipientNumber: String,
        code: String
    ): String
}