package spring.springserver.domain.auth.service.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.OAuthExchangeRequest
import spring.springserver.domain.auth.data.request.SignInRequest
import spring.springserver.domain.auth.data.request.SignUpRequest
import spring.springserver.domain.auth.data.response.SignInResponse
import spring.springserver.domain.auth.data.response.SignOutResponse
import spring.springserver.domain.auth.data.response.SignUpResponse

interface AuthService {

    fun signUp(
        signUpRequest: SignUpRequest
    ) : SignUpResponse

    fun signIn(
        signInRequest: SignInRequest,
        httpServletResponse: HttpServletResponse
    ) : SignInResponse

    fun signOut(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ) : SignOutResponse

    /**
     * 토큰 쿠키가 닿지 않는 origin으로 소셜 로그인을 마칠 때 토큰 대신 넘기는 1회용 코드.
     */
    fun issueOAuthExchangeCode(
        generateTokenRequest: GenerateTokenRequest
    ): String

    fun exchangeOAuthCode(
        oAuthExchangeRequest: OAuthExchangeRequest,
        httpServletResponse: HttpServletResponse
    ): SignInResponse

    fun verifyPassword(
        httpServletRequest: HttpServletRequest,
        rawPassword: String
    ): Boolean
}