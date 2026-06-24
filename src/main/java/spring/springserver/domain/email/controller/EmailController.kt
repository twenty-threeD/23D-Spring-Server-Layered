package spring.springserver.domain.email.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.email.data.request.CheckVerifyCodeRequest
import spring.springserver.domain.email.service.EmailService
import spring.springserver.domain.email.data.request.SendVerifyCodeRequest
import spring.springserver.domain.email.data.response.CheckVerifyCodeResponse
import spring.springserver.domain.email.data.response.SendVerifyCodeResponse
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/email")
class EmailController(private val emailService: EmailService) {

    @PostMapping("/code/send")
    fun sendVerifyCode(@Valid @RequestBody sendVerifyCodeRequest: SendVerifyCodeRequest): BaseResponse<SendVerifyCodeResponse> {

        return BaseResponse.ok(emailService.sendVerifyCode(sendVerifyCodeRequest.email))
    }

    @PostMapping("/code/verify")
    fun checkVerifyCode(@Valid @RequestBody checkVerifyCodeRequest: CheckVerifyCodeRequest): BaseResponse<CheckVerifyCodeResponse> {

        return BaseResponse.ok(emailService.checkVerifyCode(
            checkVerifyCodeRequest.email,
            checkVerifyCodeRequest.verifyCode
        ))
    }
}