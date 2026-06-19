package spring.springserver.domain.member.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.DeleteAccountResponse
import spring.springserver.domain.member.data.response.FindUsernameResponse
import spring.springserver.domain.member.data.response.PasswordResetResponse
import spring.springserver.domain.member.data.response.UsernameCheckResponse

interface MemberService {

    fun deleteAccount(httpServletRequest: HttpServletRequest,
                      httpServletResponse: HttpServletResponse): DeleteAccountResponse

    fun resetPasswordWithoutAuth(passwordResetRequest: PasswordResetRequest): PasswordResetResponse

    fun resetPasswordWithAuth(passwordResetRequest: PasswordResetRequest,
                              httpServletRequest: HttpServletRequest,
                              httpServletResponse: HttpServletResponse): PasswordResetResponse

    fun findUsername(findUsernameRequest: FindUsernameRequest): FindUsernameResponse

    fun checkUsername(username: String): UsernameCheckResponse
}