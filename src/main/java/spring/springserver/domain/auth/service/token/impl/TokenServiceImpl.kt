package spring.springserver.domain.auth.service.token.impl

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.jwt.JwtProvider
import java.util.concurrent.TimeUnit

@Service
@Transactional(rollbackFor = [Exception::class])
class TokenServiceImpl(
    private val jwtProvider: JwtProvider,
    private val redisTemplate: RedisTemplate<String, String>,
    @param:Value($$"${spring.jwt.accessTokenExpiration}") private val accessTokenExpiration: Long,
    @param:Value($$"${spring.jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Long
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

        redisTemplate.opsForValue().set(
            "refreshToken:${getTokenRequest.username}",
            refreshToken!!,
            refreshTokenExpiration,
            TimeUnit.MILLISECONDS
        )

        addCookie(
            "refreshToken",
            refreshToken,
            toCookieMaxAge(refreshTokenExpiration),
            true, httpServletResponse
        )

        return refreshToken
    }

    override fun deleteTokens(httpServletRequest: HttpServletRequest,
                              httpServletResponse: HttpServletResponse) {

        val accessToken = extractTokenFromCookie(
            "accessToken",
            httpServletRequest
        ) ?: jwtProvider.resolveToken(httpServletRequest)

        if(accessToken.isNullOrBlank() || jwtProvider.isNotValidToken(accessToken)) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        val username = jwtProvider.getUsernameFromToken(accessToken)

        val savedRefreshToken = redisTemplate.opsForValue().get("refreshToken:$username")
        val savedAccessToken = redisTemplate.opsForValue().get("accessToken:$username")

        if(savedAccessToken.isNullOrBlank() && savedAccessToken != accessToken || savedRefreshToken.isNullOrBlank()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        } else {

            addCookie(
                "accessToken",
                null,
                0,
                false,
                httpServletResponse
            )

            addCookie(
                "refreshToken",
                null,
                0,
                false,
                httpServletResponse
            )

            redisTemplate.delete("accessToken:$username")
            redisTemplate.delete("refreshToken:$username")
        }
    }

    override fun extractTokenFromCookie(cookieName: String,
                               httpServletRequest: HttpServletRequest
    ): String? {

        val cookies = httpServletRequest.cookies

        try {

            for(cookie in cookies) {

                if (cookie.name == cookieName) {

                    return cookie.value
                }
            }
        } catch (_: Exception) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return null
    }

    override fun getCurrentUsername(httpServletRequest: HttpServletRequest) : String? {

        val accessToken = extractTokenFromCookie(
            "accessToken",
            httpServletRequest
        )

        if(accessToken.isNullOrBlank()) {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return jwtProvider.getUsernameFromToken(accessToken)
    }

    private fun addCookie(
        name: String,
        value: String?,
        age: Int,
        httpOnly: Boolean,
        httpServletResponse: HttpServletResponse?
    ) {

        val cookie = Cookie(name, value)
        cookie.path = "/"
        cookie.isHttpOnly = httpOnly
        cookie.maxAge = age
        httpServletResponse?.addCookie(cookie)
    }

    private fun toCookieMaxAge(expirationMillis: Long): Int {

        return (expirationMillis / 1000).toInt()
    }
}