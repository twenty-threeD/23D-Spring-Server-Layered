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
    @param:Value($$"${app.cookie.secure}") private val cookieSecure: Boolean
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

        return refreshToken
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

        val responseCookie = ResponseCookie.from(name, value ?: "")
            .path("/")
            .httpOnly(httpOnly)
            .secure(cookieSecure || cookieSameSite.equals("None", ignoreCase = true))
            .sameSite(cookieSameSite)
            .maxAge(age.toLong())
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
