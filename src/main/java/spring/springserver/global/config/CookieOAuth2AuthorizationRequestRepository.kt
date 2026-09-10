package spring.springserver.global.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.util.Base64

/**
 * OAuth2 인가 요청(state, PKCE 등)을 세션 대신 짧은 수명의 쿠키에 담는다.
 * 세션을 쓰지 않아야 SessionCreationPolicy.STATELESS를 유지하면서도
 * 소셜 로그인 리다이렉트 왕복을 처리할 수 있다.
 *
 * 쿠키 값은 클라이언트가 임의로 조작할 수 있으므로 Java 직렬화를 쓰지 않는다.
 * 필요한 필드만 문자열 JSON으로 저장하고, 복원 시에도 문자열 맵으로만 읽어
 * 역직렬화 가젯 체인이 성립하지 않도록 한다.
 */
@Component
class CookieOAuth2AuthorizationRequestRepository(
    @param:Value($$"${app.cookie.same-site}") private val cookieSameSite: String,
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean,
    @param:Value($$"${app.cookie.domain}") private val cookieDomain: String,
    private val objectMapper: ObjectMapper
): AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    companion object {

        private const val COOKIE_NAME = "oauth2AuthorizationRequest"

        private const val COOKIE_MAX_AGE = 180L

        private const val KEY_AUTHORIZATION_URI = "authorizationUri"

        private const val KEY_CLIENT_ID = "clientId"

        private const val KEY_REDIRECT_URI = "redirectUri"

        private const val KEY_SCOPES = "scopes"

        private const val KEY_STATE = "state"

        private const val KEY_ADDITIONAL_PARAMETERS = "additionalParameters"

        private const val KEY_ATTRIBUTES = "attributes"

        private val STRING_MAP_TYPE = object: TypeReference<Map<String, Any?>>() {}
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

        val payload = mapOf(
            KEY_AUTHORIZATION_URI to authorizationRequest.authorizationUri,
            KEY_CLIENT_ID to authorizationRequest.clientId,
            KEY_REDIRECT_URI to authorizationRequest.redirectUri,
            KEY_SCOPES to authorizationRequest.scopes.toList(),
            KEY_STATE to authorizationRequest.state,
            KEY_ADDITIONAL_PARAMETERS to toStringMap(authorizationRequest.additionalParameters),
            KEY_ATTRIBUTES to toStringMap(authorizationRequest.attributes)
        )

        return Base64.getUrlEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
    }

    private fun deserialize(
        value: String
    ): OAuth2AuthorizationRequest {

        val bytes = Base64.getUrlDecoder().decode(value)

        val payload = objectMapper.readValue(bytes, STRING_MAP_TYPE)

        val authorizationUri = payload[KEY_AUTHORIZATION_URI] as? String
            ?: throw IllegalArgumentException("authorizationUri is missing")

        val clientId = payload[KEY_CLIENT_ID] as? String
            ?: throw IllegalArgumentException("clientId is missing")

        val scopes = (payload[KEY_SCOPES] as? Collection<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()

        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(authorizationUri)
            .clientId(clientId)
            .redirectUri(payload[KEY_REDIRECT_URI] as? String)
            .scopes(scopes.toSet())
            .state(payload[KEY_STATE] as? String)
            .additionalParameters(readStringMap(payload[KEY_ADDITIONAL_PARAMETERS]))
            .attributes(readStringMap(payload[KEY_ATTRIBUTES]))
            .build()
    }

    private fun toStringMap(
        source: Map<String, Any?>
    ): Map<String, String> {

        return source.entries
            .mapNotNull { entry ->

                entry.value?.let { entry.key to it.toString() }
            }
            .toMap()
    }

    private fun readStringMap(
        source: Any?
    ): Map<String, Any> {

        val map = source as? Map<*, *>
            ?: return emptyMap()

        return map.entries
            .mapNotNull { entry ->

                val key = entry.key as? String

                val value = entry.value as? String

                if (key == null || value == null) null else key to value
            }
            .toMap()
    }
}
