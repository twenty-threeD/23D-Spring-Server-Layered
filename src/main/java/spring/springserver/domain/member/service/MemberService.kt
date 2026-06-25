package spring.springserver.domain.member.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Email
import spring.springserver.domain.member.data.request.ChangeUsernameRequest
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.ChangeUsernameResponse
import spring.springserver.domain.member.data.response.CheckResponse
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse

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

    fun resetUsernameWithAuth(
        changeUsernameRequest: ChangeUsernameRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ChangeUsernameResponse

    fun checkEmail(
        email: String
    ): CheckResponse

    fun checkUsername(
        username: String
    ): CheckResponse

    fun checkPhone(
        phone: String
    ): CheckResponse
}