package spring.springserver.global.jwt

import jakarta.servlet.http.HttpServletRequest
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.member.entity.Role

interface TokenProvider {

    fun generateRefreshToken(generateTokenRequest: GenerateTokenRequest): String?

    fun generateAccessToken(generateTokenRequest: GenerateTokenRequest): String?

    fun getUsernameFromToken(token: String): String?

    fun getRole(token: String): Role?

    fun resolveToken(httpServletRequest: HttpServletRequest?): String?

    fun isNotValidToken(token: String): Boolean
}
