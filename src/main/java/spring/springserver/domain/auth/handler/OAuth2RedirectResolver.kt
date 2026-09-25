package spring.springserver.domain.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * 소셜 로그인을 시작한 프론트 origin을 콜백까지 들고 가서 그 origin으로 돌려보낸다.
 *
 * 사용자가 provider에 다녀오는 사이 요청이 끊기므로 시작 시점에 쿠키로 남겨둔다.
 * 쿠키 값은 클라이언트가 바꿀 수 있으므로 저장할 때와 꺼낼 때 모두 허용 목록과 대조한다.
 */
@Component
class OAuth2RedirectResolver(
    @param:Value($$"${app.oauth2.redirect-uri}") private val defaultRedirectUri: String,
    @param:Value($$"${app.oauth2.failure-redirect-uri}") private val defaultFailureRedirectUri: String,
    @param:Value($$"${app.oauth2.allowed-origins}") allowedOrigins: String,
    @param:Value($$"${app.cookie.same-site}") private val cookieSameSite: String,
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean,
    @param:Value($$"${app.cookie.domain}") private val cookieDomain: String
) {

    companion object {

        private const val REDIRECT_URI_PARAMETER = "redirect_uri"

        private const val COOKIE_NAME = "oauth2RedirectOrigin"

        private const val COOKIE_MAX_AGE = 180L

        private const val SUCCESS_PATH = "/oauth/success"

        private const val FAILURE_PATH = "/oauth/fail"
    }

    /**
     * 문자열 prefix로 비교하면 https://idta.store.evil.com 같은 주소가 통과하므로
     * scheme·host·port를 정규화한 origin 단위로만 비교한다.
     */
    private val allowedOrigins = allowedOrigins
        .split(",")
        .mapNotNull { toOrigin(it.trim()) }
        .toSet()

    /**
     * 로그인 시작 요청의 redirect_uri를 확인해 허용된 origin만 남긴다.
     * 값이 없거나 허용되지 않으면 이전 시도의 쿠키를 지워 기본 주소로 돌아가게 한다.
     */
    fun saveRedirectOrigin(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ) {

        val origin = toAllowedOrigin(httpServletRequest.getParameter(REDIRECT_URI_PARAMETER))

        writeCookie(
            origin ?: "",
            if (origin == null) 0 else COOKIE_MAX_AGE,
            httpServletResponse
        )
    }

    /**
     * 콜백 시점에 쿠키에서 origin을 꺼내고 쿠키는 바로 지운다.
     */
    fun consumeRedirectOrigin(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): String? {

        val value = httpServletRequest.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?.value

        if (value != null) {

            writeCookie(
                "",
                0,
                httpServletResponse
            )
        }

        return toAllowedOrigin(value)
    }

    /**
     * 토큰 쿠키가 심기는 도메인 아래의 origin이면 쿠키를 그대로 쓸 수 있다.
     * 그 밖(localhost 등)은 쿠키가 전달되지 않으므로 1회용 코드로 넘겨야 한다.
     */
    fun canReceiveTokenCookie(
        origin: String?
    ): Boolean {

        if (origin == null || cookieDomain.isBlank()) {

            return true
        }

        val host = URI(origin).host

        val domain = cookieDomain.removePrefix(".")

        return host == domain || host.endsWith(".$domain")
    }

    fun successUri(
        origin: String?
    ): String {

        return origin?.let { it + SUCCESS_PATH }
            ?: defaultRedirectUri
    }

    fun failureUri(
        origin: String?
    ): String {

        return origin?.let { it + FAILURE_PATH }
            ?: defaultFailureRedirectUri
    }

    private fun toAllowedOrigin(
        value: String?
    ): String? {

        return value
            ?.takeIf { it.isNotBlank() }
            ?.let { toOrigin(it) }
            ?.takeIf { it in allowedOrigins }
    }

    private fun toOrigin(
        value: String
    ): String? {

        return runCatching {

            val uriComponents = UriComponentsBuilder.fromUriString(value).build()

            val scheme = uriComponents.scheme?.lowercase()
                ?: return null

            val host = uriComponents.host?.lowercase()
                ?: return null

            if (scheme != "http" && scheme != "https") {

                return null
            }

            val port = uriComponents.port

            val isDefaultPort = port == -1
                || (scheme == "http" && port == 80)
                || (scheme == "https" && port == 443)

            if (isDefaultPort) "$scheme://$host" else "$scheme://$host:$port"
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
}