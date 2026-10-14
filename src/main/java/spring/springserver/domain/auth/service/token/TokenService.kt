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
     * 요청·응답 없이 해당 회원의 저장된 세션을 모두 끊는다.
     * 비밀번호 재설정처럼 로그인 상태가 아닌 경로에서 기존 refreshToken을 남기지 않기 위해 쓴다.
     */
    fun deleteTokensByMemberId(
        memberId: Long
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

    /**
     * accessToken이 지금 인증에 쓸 수 있는지 확인하고 주인의 username을 돌려준다.
     * 서명·만료뿐 아니라 토큰 종류, 로그아웃 여부, 탈퇴 여부까지 본다. 쓸 수 없으면 null이다.
     * JWT 필터를 거치지 않고 토큰을 직접 읽는 곳은 모두 이 기준을 따른다.
     */
    fun resolveActiveUsername(
        accessToken: String
    ): String?
}