package spring.springserver.domain.member.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse
import spring.springserver.domain.member.data.response.UsernameCheckResponse
import spring.springserver.domain.member.service.MemberService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/member")
class MemberController(val memberService: MemberService) {

    @PostMapping("/password/reset")
    fun resetPasswordWithoutAuth(@Valid @RequestBody passwordResetRequest: PasswordResetRequest): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(memberService.resetPasswordWithoutAuth(passwordResetRequest))
    }

    @PostMapping("/password/reset/check")
    fun resetPassword(@Valid @RequestBody passwordResetRequest: PasswordResetRequest,
                      httpServletRequest: HttpServletRequest,
                      httpServletResponse: HttpServletResponse): BaseResponse<PasswordResetResponse> {

        return BaseResponse.ok(
            memberService.resetPasswordWithAuth(
                passwordResetRequest,
                httpServletRequest,
                httpServletResponse
            )
        )
    }

    @GetMapping("/username/check")
    fun checkUsername(@RequestParam username: String): BaseResponse<UsernameCheckResponse> {

        return BaseResponse.ok(memberService.checkUsername(username))
    }

    @GetMapping("/username")
    fun findUsername(@Valid @RequestBody findUsernameRequest: FindUsernameRequest): BaseResponse<FindUsernameResponse> {

        return BaseResponse.ok(memberService.findUsername(findUsernameRequest))
    }

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
}