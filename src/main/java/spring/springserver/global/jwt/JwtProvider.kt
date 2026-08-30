package spring.springserver.global.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.member.entity.Role
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value($$"${spring.jwt.secret}") secret: String,
    @param:Value($$"${spring.jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Long,
    @param:Value($$"${spring.jwt.accessTokenExpiration}") private val accessTokenExpiration: Long
): TokenProvider {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    override fun generateRefreshToken(generateTokenRequest: GenerateTokenRequest): String? {

        val now = Date()
        val expiration = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(generateTokenRequest.username)

            .claim("tokenType", "refreshToken")

            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)

            .compact()
    }

    override fun generateAccessToken(generateTokenRequest: GenerateTokenRequest): String {

        val now = Date()
        val expiration = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .subject(generateTokenRequest.username)

            .claim("role", generateTokenRequest.role)
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

    /**
     * accessToken은 Authorization 헤더를 우선으로 하고, 없으면 httpOnly 쿠키에서 읽는다.
     * 쿠키로만 로그인한 클라이언트도 인증 필터를 통과해야 하므로 두 경로를 모두 지원한다.
     */
    override fun resolveToken(httpServletRequest: HttpServletRequest?): String? {

        val header = httpServletRequest?.getHeader("Authorization")

        return header?.takeIf { it.startsWith("Bearer ") }?.substring(7)
            ?: httpServletRequest?.cookies
                ?.firstOrNull { it.name == "accessToken" }
                ?.value
                ?.takeIf { it.isNotBlank() }
    }

    override fun isNotValidToken(token: String): Boolean {

        return runCatching {

            getClaims(token)
        }.isFailure
    }

    override fun isNotAccessToken(token: String): Boolean {

        return getClaims(token = token)?.get("tokenType", String::class.java) != "accessToken"
    }

    private fun getClaims(token: String): Claims? {

        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
