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
import spring.springserver.domain.member.repository.MemberRepository
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
    private val phoneVerifyService: PhoneVerifyService
): AuthService {

    override fun signUp(
        signUpRequest: SignUpRequest
    ): SignUpResponse {

        val phone = PhoneNormalizer.normalize(signUpRequest.phone)

        if(memberRepository.existsByUsername(signUpRequest.username)) throw ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST)
        if (memberRepository.existsByEmail(signUpRequest.email)) throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)
        if (phone != null && memberRepository.existsByPhone(phone)) throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)

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