package spring.springserver.domain.phone.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
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
        @RequestBody recipientNumber: String
    ): BaseResponse<SendPhoneVerifyNumberResponse> {

        return BaseResponse.ok(phoneVerifyService.sendMessage(recipientNumber))
    }

    @PostMapping("/verify")
    fun verifyPhone(
        @RequestBody verifyPhoneRequest: VerifyPhoneRequest
    ): BaseResponse<PhoneVerifyResponse> {

        return BaseResponse.ok(phoneVerifyService.verifyPhone(
                recipientNumber = verifyPhoneRequest.recipientNumber,
                code = verifyPhoneRequest.code
        ))
    }
}