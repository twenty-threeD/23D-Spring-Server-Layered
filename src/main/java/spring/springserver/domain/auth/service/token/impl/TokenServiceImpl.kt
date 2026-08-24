package spring.springserver.domain.auth.service.token.impl

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.ReissueTokenRequest
import spring.springserver.domain.auth.data.response.ReissueTokenResponse
import spring.springserver.domain.auth.entity.RefreshToken
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.repository.RefreshTokenRepository
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.jwt.JwtProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Service
@Transactional(rollbackFor = [Exception::class])
class TokenServiceImpl(
    private val jwtProvider: JwtProvider,
    private val redisTemplate: RedisTemplate<String, String>,
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    @param:Value($$"${spring.jwt.accessTokenExpiration}") private val accessTokenExpiration: Long,
    @param:Value($$"${spring.jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Long,
    @param:Value($$"${app.cookie.same-site}") private val cookieSameSite: String,
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean,
    @param:Value($$"${app.cookie.domain}") private val cookieDomain: String
): TokenService {

    override fun generateAccessToken(
        generateTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ): String {

        val accessToken = jwtProvider.generateAccessToken(generateTokenRequest)

        redisTemplate.opsForValue().set(
            "accessToken:${generateTokenRequest.username}",
            accessToken,
            accessTokenExpiration,
            TimeUnit.MILLISECONDS
        )

        addCookie(
            "accessToken",
            accessToken,
            toCookieMaxAge(accessTokenExpiration),
            true,
            httpServletResponse
        )

        return accessToken
    }

    override fun generateRefreshToken(getTokenRequest: GenerateTokenRequest,
                                      httpServletResponse: HttpServletResponse?
    ): String {

        val refreshToken = jwtProvider.generateRefreshToken(getTokenRequest)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(getTokenRequest.username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val expiresAt = toExpiresAt(refreshTokenExpiration)

        val saved = refreshTokenRepository.findByMemberId(member.getId()!!)

        if (saved != null) {

            saved.update(
                refreshToken,
                expiresAt
            )
        } else {

            refreshTokenRepository.save(
                RefreshToken(
                    member = member,
                    token = refreshToken,
                    expiresAt = expiresAt
                )
            )
        }

        addCookie(
            "refreshToken",
            refreshToken,
            toCookieMaxAge(refreshTokenExpiration),
            true,
            httpServletResponse
        )

        return refreshToken
    }

    /**
     * accessToken이 만료돼도 refreshToken이 살아 있으면 accessToken만 새로 발급한다.
     * refreshToken은 회전시키지 않으므로 만료 시점까지 그대로 쓴다.
     */
    override fun reissueAccessToken(
        reissueTokenRequest: ReissueTokenRequest?,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ReissueTokenResponse {

        val refreshToken = extractTokenFromCookie("refreshToken", httpServletRequest)
            ?: reissueTokenRequest?.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        if (jwtProvider.isNotValidToken(refreshToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val saved = refreshTokenRepository.findByToken(refreshToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        if (saved.isExpired()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val username = jwtProvider.getUsernameFromToken(refreshToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        /**
         * 탈취된 토큰으로 남의 accessToken을 받아가지 못하도록
         * refreshToken의 주인과 저장된 행의 주인이 같은지 확인한다.
         */
        if (saved.getMember().getId() != member.getId()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val accessToken = generateAccessToken(
            GenerateTokenRequest(
                username = member.username,
                role = member.role
            ),
            httpServletResponse
        )

        return ReissueTokenResponse.of(accessToken)
    }

    override fun deleteTokens(httpServletRequest: HttpServletRequest,
                              httpServletResponse: HttpServletResponse) {

        val accessToken = resolveAccessToken(httpServletRequest)

        if(accessToken.isNullOrBlank() || jwtProvider.isNotValidToken(accessToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val username = jwtProvider.getUsernameFromToken(accessToken)
            ?: throw ApplicationException(AuthStatusCode.INVALID_JWT)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        addCookie(
            "accessToken",
            null,
            0,
            true,
            httpServletResponse
        )

        addCookie(
            "refreshToken",
            null,
            0,
            true,
            httpServletResponse
        )

        redisTemplate.delete("accessToken:$username")
        refreshTokenRepository.deleteByMemberId(member.getId()!!)
    }

    override fun getCurrentUsername(httpServletRequest: HttpServletRequest) : String? {

        val accessToken = resolveAccessToken(httpServletRequest)

        if(accessToken.isNullOrBlank() || jwtProvider.isNotValidToken(accessToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return jwtProvider.getUsernameFromToken(accessToken)
    }

    /**
     * accessToken은 쿠키를 우선으로 하고, 없으면 Authorization 헤더에서 읽는다.
     */
    private fun resolveAccessToken(
        httpServletRequest: HttpServletRequest
    ): String? {

        return extractTokenFromCookie("accessToken", httpServletRequest)
            ?: jwtProvider.resolveToken(httpServletRequest)
    }

    private fun extractTokenFromCookie(
        cookieName: String,
        httpServletRequest: HttpServletRequest
    ): String? {

        return httpServletRequest.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }

    private fun addCookie(
        name: String,
        value: String?,
        age: Int,
        httpOnly: Boolean,
        httpServletResponse: HttpServletResponse?
    ) {

        /**
         * 도메인을 지정하지 않으면 쿠키가 발급 호스트(api.idta.store)에만 심겨
         * 프론트 도메인에서는 전송되지 않는다. 상위 도메인(.idta.store)을 지정해야
         * 서브도메인 간에 함께 실린다. 로컬처럼 값이 비어 있으면 기존대로 호스트 전용으로 둔다.
         */
        val responseCookie = ResponseCookie.from(name, value ?: "")
            .path("/")
            .httpOnly(httpOnly)
            .secure(cookieSecure || cookieSameSite.equals("None", ignoreCase = true))
            .sameSite(cookieSameSite)
            .maxAge(age.toLong())
            .also { builder ->

                if (cookieDomain.isNotBlank()) builder.domain(cookieDomain)
            }
            .build()

        httpServletResponse?.addHeader(
            HttpHeaders.SET_COOKIE,
            responseCookie.toString()
        )
    }

    private fun toCookieMaxAge(expirationMillis: Long): Int {

        return (expirationMillis / 1000).toInt()
    }

    private fun toExpiresAt(expirationMillis: Long): LocalDateTime {

        return LocalDateTime.ofInstant(
            Instant.now().plusMillis(expirationMillis),
            ZoneId.systemDefault()
        )
    }
}
