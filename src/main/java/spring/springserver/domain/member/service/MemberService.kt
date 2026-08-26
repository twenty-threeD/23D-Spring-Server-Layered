package spring.springserver.domain.member.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

interface MemberService {

    fun deleteAccount(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): DeleteAccountResponse

    fun resetPasswordWithoutAuth(
        passwordResetRequest: PasswordResetRequest
    ): PasswordResetResponse

    fun resetPasswordWithAuth(
        passwordResetRequest: PasswordResetRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): PasswordResetResponse

    fun findUsername(
        findUsernameRequest: FindUsernameRequest
    ): FindUsernameResponse

    fun checkEmail(
        email: String
    ): CheckResponse

    fun checkUsername(
        username: String
    ): UsernameCheckResponse

    fun checkPhone(
        phone: String
    ): CheckResponse

    fun ensurePhoneVerified(
        username: String
    )

    fun changeEmail(
        changeEmailRequest: ChangeEmailRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ChangeEmailResponse

    fun changePhone(
        changePhoneRequest: ChangePhoneRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ChangePhoneResponse

    fun assertEmailAvailableForChange(
        email: String,
        httpServletRequest: HttpServletRequest
    )
}