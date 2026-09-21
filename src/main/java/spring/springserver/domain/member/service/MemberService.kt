package spring.springserver.domain.member.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.member.data.request.ChangeEmailRequest
import spring.springserver.domain.member.data.request.ChangePhoneRequest
import spring.springserver.domain.member.data.request.FindUsernameRequest
import spring.springserver.domain.member.data.request.PasswordChangeRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.*
import spring.springserver.domain.member.entity.Member

interface MemberService {

    fun deleteAccount(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): DeleteAccountResponse

    fun resetPasswordWithoutAuth(
        passwordResetRequest: PasswordResetRequest
    ): PasswordResetResponse

    fun resetPasswordWithAuth(
        passwordChangeRequest: PasswordChangeRequest,
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

    /**
     * 이미 회원 엔티티를 들고 있는 호출부용. username 버전과 달리 재조회하지 않는다.
     */
    fun ensurePhoneVerified(
        member: Member
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