package spring.springserver.domain.auth.service.token

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spring.springserver.domain.auth.data.request.GenerateTokenRequest
import spring.springserver.domain.auth.data.request.ReissueTokenRequest
import spring.springserver.domain.auth.data.response.ReissueTokenResponse

interface TokenService {

    fun generateAccessToken(
        generateTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ) : String

    fun generateRefreshToken(
        getTokenRequest: GenerateTokenRequest,
        httpServletResponse: HttpServletResponse?
    ) : String

    fun reissueAccessToken(
        reissueTokenRequest: ReissueTokenRequest?,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    ): ReissueTokenResponse

    fun deleteTokens(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse
    )

    fun getCurrentUsername(
        httpServletRequest: HttpServletRequest
    ) : String?
}