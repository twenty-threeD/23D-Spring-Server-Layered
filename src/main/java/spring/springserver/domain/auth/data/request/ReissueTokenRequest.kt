package spring.springserver.domain.auth.data.request

/**
 * refreshToken은 쿠키로도 내려가므로, 쿠키를 쓰는 클라이언트는 본문 없이 호출할 수 있다.
 */
data class ReissueTokenRequest(
    val refreshToken: String? = null
)
