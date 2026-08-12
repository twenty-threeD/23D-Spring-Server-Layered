package spring.springserver.domain.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.member.entity.Role
import spring.springserver.global.exception.exception.ApplicationException

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Component
class OAuth2SuccessHandler(
    private val tokenService: TokenService,
    @param:Value($$"${app.oauth2.redirect-uri}") private val redirectUri: String
): AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        authentication: Authentication
    ) {

        val oAuth2User = authentication.principal as? OAuth2User
            ?: throw ApplicationException(AuthStatusCode.UNKNOWN_REGISTRATION_ID)

        val generateTokenRequest = GenerateTokenRequest(
            oAuth2User.attributes["username"].toString(),
            Role.valueOf(oAuth2User.attributes["role"].toString())
        )

        val accessToken = tokenService.generateAccessToken(
            generateTokenRequest,
            httpServletResponse
        )

        tokenService.generateRefreshToken(
            generateTokenRequest,
            httpServletResponse
        )

        val successRedirectUri = UriComponentsBuilder
            .fromUriString(redirectUri)
            .queryParam("accessToken", accessToken)
            .build()
            .encode()
            .toUriString()

        httpServletResponse.sendRedirect(successRedirectUri)
    }
}
