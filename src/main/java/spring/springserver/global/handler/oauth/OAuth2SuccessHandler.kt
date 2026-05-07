package spring.springserver.global.handler.oauth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import spring.springserver.domain.oauth.data.response.OAuthResponse
import spring.springserver.global.data.BaseResponse

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
@Component
class OAuth2SuccessHandler(private val objectMapper: ObjectMapper): AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        httpServletRequest: HttpServletRequest?,
        httpServletResponse: HttpServletResponse?,
        authentication: Authentication?
    ) {

        val oAuth2User = authentication?.principal as OAuth2User

        val email = oAuth2User.attributes["email"].toString()
        val name = oAuth2User.attributes["name"].toString()

        val oAuthResponse = OAuthResponse(
            email,
            name
        )

        httpServletResponse?.contentType = "application/json;charset=UTF-8"
        httpServletResponse?.status = HttpServletResponse.SC_OK
        httpServletResponse?.writer?.write(objectMapper.writeValueAsString(BaseResponse.ok(oAuthResponse)))
    }
}