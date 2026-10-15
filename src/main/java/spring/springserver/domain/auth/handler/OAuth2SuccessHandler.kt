package spring.springserver.domain.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.auth.AuthService
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.member.entity.Role

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Component
class OAuth2SuccessHandler(
    private val tokenService: TokenService,
    private val authService: AuthService,
    private val oAuth2RedirectResolver: OAuth2RedirectResolver
): AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        authentication: Authentication
    ) {

        val origin = oAuth2RedirectResolver.consumeRedirectOrigin(
            httpServletRequest,
            httpServletResponse
        )

        val oAuth2User = authentication.principal as? OAuth2User

        if (oAuth2User == null) {

            redirectToFailure(
                AuthStatusCode.UNKNOWN_REGISTRATION_ID.getCode(),
                origin,
                httpServletResponse
            )

            return
        }

        val generateTokenRequest = GenerateTokenRequest(
            oAuth2User.attributes["username"].toString(),
            Role.valueOf(oAuth2User.attributes["role"].toString())
        )

        /**
         * 토큰 쿠키가 닿지 않는 origin(localhost 등)에는 쿠키 대신 1회용 코드를 넘기고,
         * 프론트가 자기 origin에서 코드를 교환해 쿠키를 받게 한다.
         */
        if (!oAuth2RedirectResolver.canReceiveTokenCookie(origin)) {

            val redirectUri = UriComponentsBuilder
                .fromUriString(oAuth2RedirectResolver.successUri(origin))
                .queryParam("code", authService.issueOAuthExchangeCode(generateTokenRequest))
                .build()
                .encode()
                .toUriString()

            httpServletResponse.sendRedirect(redirectUri)

            return
        }

        /**
         * 토큰은 httpOnly 쿠키로만 내려보낸다.
         * 쿼리파라미터로 넘기면 accessToken이 브라우저 히스토리, Referer 헤더,
         * 프록시·서버 접근 로그에 평문으로 남는다.
         */
        tokenService.generateAccessToken(
            generateTokenRequest,
            httpServletResponse
        )

        tokenService.generateRefreshToken(
            generateTokenRequest,
            httpServletResponse
        )

        httpServletResponse.sendRedirect(oAuth2RedirectResolver.successUri(origin))
    }

    private fun redirectToFailure(
        errorCode: String,
        origin: String?,
        httpServletResponse: HttpServletResponse
    ) {

        val failureUri = UriComponentsBuilder
            .fromUriString(oAuth2RedirectResolver.failureUri(origin))
            .queryParam("code", errorCode)
            .build()
            .encode()
            .toUriString()

        httpServletResponse.sendRedirect(failureUri)
    }
}