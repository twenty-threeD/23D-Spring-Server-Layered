package spring.springserver.domain.member.service.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.email.service.EmailService
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
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.entity.Provider
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.service.MemberService
import spring.springserver.domain.phone.service.PhoneVerifyService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import spring.springserver.global.util.PhoneNormalizer

@Service
@Transactional(rollbackFor = [Exception::class])
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService,
    private val emailService: EmailService,
    private val phoneVerifyService: PhoneVerifyService,
    private val passwordEncoder: PasswordEncoder
) : MemberService {

    override fun deleteAccount(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): DeleteAccountResponse {

        val username = tokenService.getCurrentUsername(httpServletRequest)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse,
        )

        memberRepository.delete(member)

        return DeleteAccountResponse.of("탈퇴되었습니다.")
    }

    override fun resetPasswordWithoutAuth(
        passwordResetRequest: PasswordResetRequest
    ): PasswordResetResponse {

        val member = memberRepository.findByUsername(passwordResetRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        member.password = passwordEncoder.encode(passwordResetRequest.newPassword)

        return PasswordResetResponse.of("비밀번호가 변경되었습니다.")
    }

    override fun resetPasswordWithAuth(
        passwordResetRequest: PasswordResetRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): PasswordResetResponse {

        val username = tokenService.getCurrentUsername(httpServletRequest)

        if(username.isNullOrBlank()) throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(passwordResetRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val encoded = passwordEncoder.encode(passwordResetRequest.newPassword)
        member.password = encoded

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse,
        )

        return PasswordResetResponse.of("비밀번호가 변경되었습니다. 다시 로그인 해주세요.")
    }

    override fun findUsername(
        findUsernameRequest: FindUsernameRequest
    ): FindUsernameResponse {

        val username = memberRepository.findUsernameByEmail(findUsernameRequest.email)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        return FindUsernameResponse.of(username)
    }

    @Transactional(readOnly = true)
    override fun checkEmail(
        email: String
    ): CheckResponse {

        if (memberRepository.existsByEmail(email)) throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)

        return CheckResponse.of("사용할 수 있는 이메일입니다.")
    }

    @Transactional(readOnly = true)
    override fun checkPhone(
        phone: String
        ): CheckResponse {

        if (memberRepository.existsByPhone(PhoneNormalizer.normalize(phone)
                ?: throw ApplicationException(CommonStatusCode.INVALID_ARGUMENT))
            ) throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)

        return CheckResponse.of("사용할 수 있는 전화번호입니다.")
    }

    @Transactional(readOnly = true)
    override fun checkUsername(
        username: String
    ): UsernameCheckResponse {

        if (memberRepository.existsByUsername(username)) throw ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST)

        return UsernameCheckResponse.of("사용 가능한 사용자명입니다.")
    }

    @Transactional(readOnly = true)
    override fun ensurePhoneVerified(
        username: String
    ) {

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (!member.isPhoneVerified()) {

            throw ApplicationException(MemberStatusCode.PHONE_NOT_VERIFIED)
        }
    }

    override fun changeEmail(
        changeEmailRequest: ChangeEmailRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ChangeEmailResponse {

        val member = getAuthenticatedMember(httpServletRequest)

        verifyPasswordIfLocal(
            member,
            changeEmailRequest.password
        )

        if (member.email != changeEmailRequest.newEmail && memberRepository.existsByEmail(changeEmailRequest.newEmail)) {

            throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)
        }

        emailService.checkChangeEmailCode(
            changeEmailRequest.newEmail,
            changeEmailRequest.verifyCode
        )

        member.changeEmail(email = changeEmailRequest.newEmail)

        try {

            memberRepository.saveAndFlush(member)
        } catch (_: DataIntegrityViolationException) {

            throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)
        }

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse
        )

        return ChangeEmailResponse.of("이메일이 변경되었습니다. 다시 로그인 해주세요.")
    }

    override fun changePhone(
        changePhoneRequest: ChangePhoneRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ChangePhoneResponse {

        val member = getAuthenticatedMember(httpServletRequest)

        verifyPasswordIfLocal(
            member,
            changePhoneRequest.password
        )

        val normalizedPhone = PhoneNormalizer.normalize(changePhoneRequest.newPhone)
            ?: throw ApplicationException(CommonStatusCode.INVALID_ARGUMENT)

        if (member.phone != normalizedPhone && memberRepository.existsByPhone(normalizedPhone)) {

            throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)
        }

        phoneVerifyService.verifyCodeOnly(
            recipientNumber = changePhoneRequest.newPhone,
            code = changePhoneRequest.code
        )

        member.changePhone(phone = normalizedPhone)

        try {

            memberRepository.saveAndFlush(member)
        } catch (_: DataIntegrityViolationException) {

            throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)
        }

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse
        )

        return ChangePhoneResponse.of("전화번호가 변경되었습니다. 다시 로그인 해주세요.")
    }

    private fun getAuthenticatedMember(
        httpServletRequest: HttpServletRequest
    ): Member {

        val username = tokenService.getCurrentUsername(httpServletRequest)

        if (username.isNullOrBlank()) throw ApplicationException(AuthStatusCode.INVALID_JWT)

        return memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
    }

    @Transactional(readOnly = true)
    override fun assertEmailAvailableForChange(
        email: String,
        httpServletRequest: HttpServletRequest
    ) {

        val member = getAuthenticatedMember(httpServletRequest)

        if (member.email != email && memberRepository.existsByEmail(email)) {

            throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)
        }
    }

    private fun verifyPasswordIfLocal(
        member: Member,
        rawPassword: String
    ) {

        if (member.provider != Provider.AUTH) return

        val encodedPassword = member.password

        if (encodedPassword.isNullOrBlank() || !passwordEncoder.matches(rawPassword, encodedPassword)) {

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }
    }
}
