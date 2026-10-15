package spring.springserver.domain.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import spring.springserver.domain.auth.exception.AuthStatusCode

@Component
class OAuth2FailureHandler(
    private val oAuth2RedirectResolver: OAuth2RedirectResolver
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

        val origin = oAuth2RedirectResolver.consumeRedirectOrigin(
            httpServletRequest,
            httpServletResponse
        )

        val redirectUri = UriComponentsBuilder
            .fromUriString(oAuth2RedirectResolver.failureUri(origin))
            .queryParam("code", errorCode)
            .build()
            .encode()
            .toUriString()

        httpServletResponse.sendRedirect(redirectUri)
    }
}
