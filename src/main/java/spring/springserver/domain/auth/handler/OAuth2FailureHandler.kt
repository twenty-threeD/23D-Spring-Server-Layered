package spring.springserver.domain.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import spring.springserver.domain.auth.exception.AuthStatusCode

@Component
class OAuth2FailureHandler(
    @param:Value($$"${app.oauth2.failure-redirect-uri}") private val failureRedirectUri: String
): AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {

        val errorCode = (authenticationException as? OAuth2AuthenticationException)
            ?.error
            ?.errorCode
            ?.takeIf { it.isNotBlank() }
            ?: AuthStatusCode.OAUTH_LOGIN_FAILED.getCode()

        val redirectUri = UriComponentsBuilder
            .fromUriString(failureRedirectUri)
            .queryParam("code", errorCode)
            .build()
            .encode()
            .toUriString()

        httpServletResponse.sendRedirect(redirectUri)
    }
}
