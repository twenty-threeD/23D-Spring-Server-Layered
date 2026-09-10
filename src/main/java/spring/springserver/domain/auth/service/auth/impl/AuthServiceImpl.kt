package spring.springserver.domain.auth.service.auth.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.SignInRequest
import spring.springserver.domain.auth.data.request.SignUpRequest
import spring.springserver.domain.auth.data.response.SignInResponse
import spring.springserver.domain.auth.data.response.SignOutResponse
import spring.springserver.domain.auth.data.response.SignUpResponse
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.auth.AuthService
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.key.service.KeyService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.member.retention.MemberRetentionService
import spring.springserver.domain.phone.service.PhoneVerifyService
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.PhoneNormalizer

@Service
@Transactional(rollbackFor = [Exception::class])
class AuthServiceImpl(
    private val passwordEncoder: PasswordEncoder,
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService,
    private val keyService: KeyService,
    private val profileService: ProfileService,
    private val phoneVerifyService: PhoneVerifyService,
    private val memberRetentionService: MemberRetentionService
): AuthService {

    override fun signUp(
        signUpRequest: SignUpRequest
    ): SignUpResponse {

        val phone = PhoneNormalizer.normalize(signUpRequest.phone)

        /**
         * 탈퇴 회원도 행이 남고 username·email·phone의 unique 제약이 그대로라 재가입이 막힌다.
         * 제한 기간(MemberRetentionService.RETENTION_DAYS)이 지났으면 값을 풀어 가입시키고,
         * 기간 중이면 "이미 존재"와 구분되는 코드로 탈퇴 계정임을 알려준다.
         */
        checkDuplicate(
            member = memberRepository.findByUsername(signUpRequest.username),
            duplicateStatusCode = AuthStatusCode.USERNAME_ALREADY_EXIST
        )

        checkDuplicate(
            member = memberRepository.findByEmail(signUpRequest.email),
            duplicateStatusCode = AuthStatusCode.EMAIL_ALREADY_EXIST
        )

        if (phone != null) {

            checkDuplicate(
                member = memberRepository.findByPhone(phone),
                duplicateStatusCode = AuthStatusCode.PHONE_ALREADY_EXIST
            )
        }

        val newMember = signUpRequest.toEntity(
            encodedPassword = passwordEncoder.encode(signUpRequest.password),
            normalizedPhone = phone
        )

        /**
         * 가입 직전에 로그인 없이 본인인증을 마쳤다면 그 표식을 소비해 인증 상태로 가입시킨다.
         * 건너뛰었다면 미인증 상태로 가입되고, 나중에 로그인 후 인증하면 된다.
         */
        if (phone != null && phoneVerifyService.consumePhoneVerification(phone = phone)) {

            newMember.verifyPhone(phone = phone)
        }

        val member = memberRepository.save(newMember)

        keyService.generateKeyPair(
            memberId = member.getId()!!
        )

        profileService.createDefaultProfile(member)

        return SignUpResponse.of("회원가입이 완료 되었습니다.")
    }

    override fun signIn(
        signInRequest: SignInRequest,
        httpServletResponse: HttpServletResponse
    ): SignInResponse {

        val member = memberRepository.findByEmail(signInRequest.email)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        /**
         * 탈퇴한 회원은 행이 남아 있으므로 로그인 단계에서 걸러낸다.
         * 탈퇴 여부를 알려주지 않기 위해 자격 증명 오류와 같은 응답을 준다.
         */
        if (member.isDeleted()) {

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }

        if(!passwordEncoder.matches(signInRequest.password, member.password)) {

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }

        val generateTokenRequest = GenerateTokenRequest(
            member.username,
            member.role
        )

        return SignInResponse.of(
            tokenService.generateAccessToken(
                generateTokenRequest,
                httpServletResponse
            ),
            tokenService.generateRefreshToken(
                generateTokenRequest,
                httpServletResponse
            )
        )
    }

    override fun signOut(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): SignOutResponse {

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse
        )

        return SignOutResponse.of("로그아웃 되었습니다.")
    }

    /**
     * 가입에 쓰려는 값이 이미 쓰이고 있으면 예외를 던진다.
     * 그 값을 쥐고 있는 회원이 탈퇴 회원이면 재가입 불가임을 구분해 알려준다.
     */
    private fun checkDuplicate(
        member: Member?,
        duplicateStatusCode: AuthStatusCode
    ) {

        if (member == null) {

            return
        }

        if (member.isDeleted()) {

            /**
             * 재가입 제한 기간이 지났으면 그 자리에서 값을 풀어주고 가입을 통과시킨다.
             */
            if (memberRetentionService.releaseIfRetentionExpired(member.getId()!!)) {

                return
            }

            throw ApplicationException(AuthStatusCode.WITHDRAWN_ACCOUNT_CANNOT_REJOIN)
        }

        throw ApplicationException(duplicateStatusCode)
    }

    override fun verifyPassword(
        httpServletRequest: HttpServletRequest,
        rawPassword: String
    ): Boolean {

        return passwordEncoder.matches(rawPassword,
            memberRepository.findByUsername(
                tokenService.getCurrentUsername(httpServletRequest)
            )?.password
        )
    }
}