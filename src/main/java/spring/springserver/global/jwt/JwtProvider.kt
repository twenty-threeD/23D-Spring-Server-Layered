package spring.springserver.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.interfaces.TokenProvider
import spring.springserver.domain.member.entity.Role
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(@Value($$"${spring.jwt.secret}") secret: String,
                  @param:Value($$"${spring.jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Long,
                  @param:Value($$"${spring.jwt.accessTokenExpiration}") private val accessTokenExpiration: Long): TokenProvider {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    override fun generateRefreshToken(generateTokenRequest: GenerateTokenRequest?): String? {

        val now = Date()
        val expiration = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(generateTokenRequest?.username)

            .claim("tokenType", "refreshToken")

            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)

            .compact()
    }

    override fun generateAccessToken(generateTokenRequest: GenerateTokenRequest?): String? {

        val now = Date()
        val expiration = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .subject(generateTokenRequest?.username)

            .claim("role", generateTokenRequest?.role)
            .claim("tokenType", "accessToken")

            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)

            .compact()
    }

    override fun getUsernameFromToken(token: String): String? {

        return getClaims(token)?.subject
    }

    override fun getRole(token: String): Role? {

        val role = getClaims(token)?.get("role", String::class.java)

        return role?.let { Role.valueOf(role) }
    }

    override fun resolveToken(httpServletRequest: HttpServletRequest?): String? {

        val token = httpServletRequest?.getHeader("Authorization")

        return token?.takeIf { it.startsWith("Bearer ") }?.substring(7)
    }

    // 해당 메서드는 유효한 경우 항상 false를, 유효하지 않은 경우 true를 반환합니다.
    override fun isNotValidToken(token: String?): Boolean {

        return runCatching {

            token?.let { getClaims(it) }
        }.isFailure
    }

    private fun getClaims(token: String): Claims? {

        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}