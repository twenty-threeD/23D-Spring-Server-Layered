package spring.springserver.domain.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.service.token.impl.TokenServiceImpl
import spring.springserver.domain.member.entity.Role
import spring.springserver.domain.oauth.data.response.OAuthResponse
import spring.springserver.global.data.BaseResponse

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Component
class OAuth2SuccessHandler(private val objectMapper: ObjectMapper,
                           private val tokenService: TokenServiceImpl): AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        httpServletRequest: HttpServletRequest?,
        httpServletResponse: HttpServletResponse?,
        authentication: Authentication?) {

        val oAuth2User = authentication?.principal as OAuth2User

        val username = oAuth2User.attributes["username"].toString()
        val role = Role.valueOf(oAuth2User.attributes["role"].toString())

        val generateTokenRequest = GenerateTokenRequest(
            username,
            role
        )

        val accessToken = tokenService.generateAccessToken(
            generateTokenRequest,
            httpServletResponse
        )
        val refreshToken = tokenService.generateRefreshToken(
            generateTokenRequest,
            httpServletResponse
        )

        httpServletResponse?.contentType = "application/json;charset=UTF-8"
        httpServletResponse?.status = HttpServletResponse.SC_OK
        httpServletResponse?.writer?.write(objectMapper.writeValueAsString(
            BaseResponse.ok(
                OAuthResponse.of(
                    accessToken,
                    refreshToken
                )
            )
        ))
    }
}