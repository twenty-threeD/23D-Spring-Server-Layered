package spring.springserver.domain.member.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.member.data.request.ChangeEmailRequest
import spring.springserver.domain.member.data.request.ChangePhoneRequest
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.ChangeEmailResponse
import spring.springserver.domain.member.data.response.ChangePhoneResponse
import spring.springserver.domain.member.data.response.CheckResponse
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse
import spring.springserver.domain.member.data.response.UsernameCheckResponse
import spring.springserver.domain.member.service.MemberService
import spring.springserver.global.data.BaseResponse

@RestController
@Validated
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService
) {

    @PostMapping("/password/reset")
    fun resetPasswordWithoutAuth(
        @Valid @RequestBody passwordResetRequest: PasswordResetRequest
    ): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(memberService.resetPasswordWithoutAuth(passwordResetRequest))
    }

    @PostMapping("/password/reset/check")
    fun resetPassword(
        @Valid @RequestBody passwordResetRequest: PasswordResetRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(
            memberService.resetPasswordWithAuth(
                passwordResetRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @GetMapping("/username")
    fun findUsername(
        @Valid @RequestBody findUsernameRequest: FindUsernameRequest
    ): BaseResponse<FindUsernameResponse> {

        return BaseResponse.ok(memberService.findUsername(findUsernameRequest))
    }

    @DeleteMapping("/account")
    fun deleteAccount(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<DeleteAccountResponse> {

        return BaseResponse.ok(
            memberService.deleteAccount(
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @GetMapping("/check-email")
    fun checkEmail(
        @RequestParam @Email @NotBlank email: String
    ): BaseResponse<CheckResponse> {

        return BaseResponse.ok(memberService.checkEmail(email))
    }

    @GetMapping("/check-phone")
    fun checkPhone(
        @RequestParam @NotBlank phone: String
    ): BaseResponse<CheckResponse> {

        return BaseResponse.ok(memberService.checkPhone(phone))
    }

    @GetMapping("/check-username")
    fun checkUsername(
        @RequestParam @NotBlank username: String
    ): BaseResponse<UsernameCheckResponse> {

        return BaseResponse.ok(memberService.checkUsername(username))
    }

    @PatchMapping("/email")
    fun changeEmail(
        @Valid @RequestBody changeEmailRequest: ChangeEmailRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<ChangeEmailResponse> {

        return BaseResponse.ok(
            memberService.changeEmail(
                changeEmailRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @PatchMapping("/phone")
    fun changePhone(
        @Valid @RequestBody changePhoneRequest: ChangePhoneRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): BaseResponse<ChangePhoneResponse> {

        return BaseResponse.ok(
            memberService.changePhone(
                changePhoneRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }
}