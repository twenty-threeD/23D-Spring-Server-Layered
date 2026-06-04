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
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class AuthServiceImpl(
    private val passwordEncoder: PasswordEncoder,
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService
) : AuthService {

    override fun signUp(signUpRequest: SignUpRequest): SignUpResponse {

        if(memberRepository.existsByUsername(signUpRequest.username)){

            throw ApplicationException(AuthStatusCode.USERNAME_ALREADY_EXIST)
        }

        if (memberRepository.existsByEmail(signUpRequest.email)) {

            throw ApplicationException(AuthStatusCode.EMAIL_ALREADY_EXIST)
        }

        if (memberRepository.existsByPhone(signUpRequest.phone)) {

            throw ApplicationException(AuthStatusCode.PHONE_ALREADY_EXIST)
        }

        memberRepository.save(signUpRequest.toEntity(passwordEncoder.encode(signUpRequest.password)))

        return SignUpResponse.Companion.of("회원가입이 완료 되었습니다.")
    }

    override fun signIn(signInRequest: SignInRequest,
                        httpServletResponse: HttpServletResponse
    ): SignInResponse {

        val member = memberRepository.findByUsername(signInRequest.username)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        if(!passwordEncoder.matches(signInRequest.password, member.password)){

            throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)
        }

        val generateTokenRequest = GenerateTokenRequest(
            member.username,
            member.role
        )

        return SignInResponse.Companion.of(
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

    override fun signOut(httpServletRequest: HttpServletRequest,
                         httpServletResponse: HttpServletResponse
    ): SignOutResponse {

        tokenService.deleteTokens(
            httpServletRequest,
            httpServletResponse
        )

        return SignOutResponse.Companion.of("로그아웃 되었습니다.")
    }
}