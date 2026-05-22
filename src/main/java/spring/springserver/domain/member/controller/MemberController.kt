package spring.springserver.domain.member.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.member.data.request.ChangeUsernameRequest
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.ChangeUsernameResponse
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse
import spring.springserver.domain.member.service.MemberService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/member")
class MemberController(val memberService: MemberService) {

    @DeleteMapping("/account")
    fun deleteAccount(httpServletRequest: HttpServletRequest,
                            httpServletResponse: HttpServletResponse): BaseResponse<DeleteAccountResponse> {

        return BaseResponse.ok(
            memberService.deleteAccount(
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @PostMapping("/password/reset")
    fun resetPasswordWithoutAuth(@Valid @RequestBody passwordResetRequest: PasswordResetRequest): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(memberService.resetPasswordWithoutAuth(passwordResetRequest))
    }

    @PostMapping("/password/reset/check")
    fun resetPassword(@Valid @RequestBody passwordResetRequest: PasswordResetRequest,
                                                httpServletResponse: HttpServletResponse,
                                                httpServletRequest: HttpServletRequest): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(
            memberService.resetPasswordWithAuth(
                passwordResetRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @GetMapping("/username")
    fun findUsername(@Valid @RequestBody findUsernameRequest: FindUsernameRequest): BaseResponse<FindUsernameResponse> {

        return BaseResponse.ok(memberService.findUsername(findUsernameRequest))
    }

    @PostMapping("/username/reset")
    fun resetUsernameWithAuth(@Valid @RequestBody changeUsernameRequest: ChangeUsernameRequest,
                                                        httpServletRequest: HttpServletRequest,
                                                        httpServletResponse: HttpServletResponse): BaseResponse<ChangeUsernameResponse> {

        return BaseResponse.ok(memberService.resetUsernameWithAuth(changeUsernameRequest,
                                                                          httpServletRequest,
                                                                          httpServletResponse)
       )
    }
}