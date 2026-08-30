package spring.springserver.global.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

/**
 * OAuth2 인가 요청(state, PKCE 등)을 세션 대신 짧은 수명의 쿠키에 담는다.
 * 세션을 쓰지 않아야 SessionCreationPolicy.STATELESS를 유지하면서도
 * 소셜 로그인 리다이렉트 왕복을 처리할 수 있다.
 */
@Component
class CookieOAuth2AuthorizationRequestRepository(
    @param:Value($$"${app.cookie.same-site}") private val cookieSameSite: String,
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean,
    @param:Value($$"${app.cookie.domain}") private val cookieDomain: String
): AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    companion object {

        private const val COOKIE_NAME = "oauth2AuthorizationRequest"

        private const val COOKIE_MAX_AGE = 180L
    }

    override fun loadAuthorizationRequest(
        httpServletRequest: HttpServletRequest
    ): OAuth2AuthorizationRequest? {

        return readAuthorizationRequest(httpServletRequest)
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ) {

        if (authorizationRequest == null) {

            writeCookie(
                "",
                0,
                httpServletResponse
            )

            return
        }

        writeCookie(
            serialize(authorizationRequest),
            COOKIE_MAX_AGE,
            httpServletResponse
        )
    }

    override fun removeAuthorizationRequest(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): OAuth2AuthorizationRequest? {

        val authorizationRequest = readAuthorizationRequest(httpServletRequest)

        writeCookie(
            "",
            0,
            httpServletResponse
        )

        return authorizationRequest
    }

    private fun readAuthorizationRequest(
        httpServletRequest: HttpServletRequest
    ): OAuth2AuthorizationRequest? {

        val value = httpServletRequest.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return runCatching {

            deserialize(value)
        }.getOrNull()
    }

    private fun writeCookie(
        value: String,
        age: Long,
        httpServletResponse: HttpServletResponse
    ) {

        val responseCookie = ResponseCookie.from(COOKIE_NAME, value)
            .path("/")
            .httpOnly(true)
            .secure(cookieSecure || cookieSameSite.equals("None", ignoreCase = true))
            .sameSite(cookieSameSite)
            .maxAge(age)
            .also { builder ->

                if (cookieDomain.isNotBlank()) builder.domain(cookieDomain)
            }
            .build()

        httpServletResponse.addHeader(
            HttpHeaders.SET_COOKIE,
            responseCookie.toString()
        )
    }

    private fun serialize(
        authorizationRequest: OAuth2AuthorizationRequest
    ): String {

        val byteArrayOutputStream = ByteArrayOutputStream()

        ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->

            objectOutputStream.writeObject(authorizationRequest)
        }

        return Base64.getUrlEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    private fun deserialize(
        value: String
    ): OAuth2AuthorizationRequest {

        val bytes = Base64.getUrlDecoder().decode(value)

        return ObjectInputStream(ByteArrayInputStream(bytes)).use { objectInputStream ->

            objectInputStream.readObject() as OAuth2AuthorizationRequest
        }
    }
}
