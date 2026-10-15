package spring.springserver.domain.phone.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.phone.data.request.SendPhoneVerifyNumberRequest
import spring.springserver.domain.phone.data.request.VerifyPhoneRequest
import spring.springserver.domain.phone.data.response.PhoneVerifyResponse
import spring.springserver.domain.phone.data.response.SendPhoneVerifyNumberResponse
import spring.springserver.domain.phone.service.PhoneVerifyService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/phone")
class PhoneController(
    private val phoneVerifyService: PhoneVerifyService
) {

    @PostMapping("/send")
    fun sendVerify(
        @Valid @RequestBody sendPhoneVerifyNumberRequest: SendPhoneVerifyNumberRequest
    ): BaseResponse<SendPhoneVerifyNumberResponse> {

        return BaseResponse.ok(phoneVerifyService.sendMessage(
                recipientNumber = sendPhoneVerifyNumberRequest.recipientNumber
        ))
    }

    @PostMapping("/verify")
    fun verifyPhone(
        @Valid @RequestBody verifyPhoneRequest: VerifyPhoneRequest
    ): BaseResponse<PhoneVerifyResponse> {

        return BaseResponse.ok(phoneVerifyService.verifyPhone(
                recipientNumber = verifyPhoneRequest.recipientNumber,
                code = verifyPhoneRequest.code
        ))
    }
}