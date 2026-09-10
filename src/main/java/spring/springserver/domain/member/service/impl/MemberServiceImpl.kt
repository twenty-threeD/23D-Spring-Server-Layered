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
import spring.springserver.domain.member.data.request.PasswordChangeRequest
import spring.springserver.domain.member.data.request.PasswordResetRequest
import spring.springserver.domain.member.data.response.*
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

        /**
         * 계약·견적 등 거래 기록이 member를 참조하고 있어 행을 지우지 않고 탈퇴 표시만 남긴다.
         * 저장된 리프레시 토큰은 위 deleteTokens에서 이미 지워진다.
         */
        member.withdraw()

        memberRepository.save(member)

        return DeleteAccountResponse.of("탈퇴되었습니다.")
    }

    override fun resetPasswordWithoutAuth(
        passwordResetRequest: PasswordResetRequest
    ): PasswordResetResponse {

        val member = memberRepository.findByUsername(passwordResetRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        if (member.provider != Provider.AUTH) {

            throw ApplicationException(MemberStatusCode.SOCIAL_ACCOUNT_CANNOT_RESET_PASSWORD)
        }

        verifyPasswordResetOwnership(
            member,
            passwordResetRequest
        )

        member.password = passwordEncoder.encode(passwordResetRequest.newPassword)

        return PasswordResetResponse.of("비밀번호가 변경되었습니다.")
    }

    override fun resetPasswordWithAuth(
        passwordChangeRequest: PasswordChangeRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): PasswordResetResponse {

        /**
         * 변경 대상은 항상 토큰의 주체다.
         * 요청 본문으로 다른 계정을 지정할 수 없다.
         */
        val member = getAuthenticatedMember(httpServletRequest)

        ensureLocalAndVerifyPassword(
            member,
            passwordChangeRequest.currentPassword
        )

        member.password = passwordEncoder.encode(passwordChangeRequest.newPassword)

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

        ensureLocalAndVerifyPassword(
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

        ensureLocalAndVerifyPassword(
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

    /**
     * 비로그인 재설정의 유일한 소유권 증거다.
     * 휴대폰 또는 이메일로 발송된 인증코드를 먼저 소비한 뒤,
     * 인증에 성공한 연락처가 실제로 그 계정의 것인지 확인한다.
     */
    private fun verifyPasswordResetOwnership(
        member: Member,
        passwordResetRequest: PasswordResetRequest
    ) {

        val verifyCode = passwordResetRequest.verifyCode

        if (verifyCode.isNullOrBlank()) throw ApplicationException(MemberStatusCode.VERIFICATION_REQUIRED)

        val phone = passwordResetRequest.phone
        val email = passwordResetRequest.email

        when {

            !phone.isNullOrBlank() -> {

                val verifiedPhone = phoneVerifyService.verifyCodeOnly(
                    recipientNumber = phone,
                    code = verifyCode
                )

                if (member.phone != verifiedPhone) {

                    throw ApplicationException(MemberStatusCode.VERIFICATION_TARGET_MISMATCH)
                }
            }

            !email.isNullOrBlank() -> {

                emailService.checkVerifyCode(
                    email,
                    verifyCode
                )

                if (!member.email.equals(email, ignoreCase = true)) {

                    throw ApplicationException(MemberStatusCode.VERIFICATION_TARGET_MISMATCH)
                }
            }

            else -> throw ApplicationException(MemberStatusCode.VERIFICATION_REQUIRED)
        }
    }

    private fun ensureLocalAndVerifyPassword(
        member: Member,
        rawPassword: String
    ) {

        if (member.provider != Provider.AUTH) {

            throw ApplicationException(MemberStatusCode.SOCIAL_ACCOUNT_CANNOT_CHANGE)
        }

        val encodedPassword = member.password

        if (encodedPassword.isNullOrBlank() || !passwordEncoder.matches(rawPassword, encodedPassword)) {

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }
    }
}
