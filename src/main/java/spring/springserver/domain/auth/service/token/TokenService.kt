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

    /**
     * 로그아웃 등으로 무효화된 accessToken인지 확인한다.
     * JWT는 만료 전까지 서명만으로 유효하므로 서버가 거부 목록을 따로 들고 있어야 한다.
     */
    fun isRevokedAccessToken(
        accessToken: String
    ): Boolean

    fun getCurrentUsername(
        httpServletRequest: HttpServletRequest
    ) : String?
}