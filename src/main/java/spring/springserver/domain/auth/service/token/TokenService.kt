package spring.springserver.domain.auth.service.token

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.auth.data.request.GenerateTokenRequest

interface TokenService {

    fun generateAccessToken(
        generateTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ) : String

    fun generateRefreshToken(
        getTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ) : String

    fun deleteTokens(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    )

    fun extractTokenFromCookie(
        cookieName: String,
        httpServletRequest: HttpServletRequest
    ) : String?

    fun getCurrentUsername(
        httpServletRequest: HttpServletRequest
    ) : String?
}