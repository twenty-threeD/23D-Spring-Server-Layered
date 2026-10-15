package spring.springserver.global.util

import jakarta.servlet.http.HttpServletRequest

/**
 * 리버스 프록시 뒤에서 돌 수 있으므로 X-Forwarded-For를 우선으로 보고,
 * 없으면 remoteAddr을 쓴다. XFF는 쉼표 구분 체인이고 가장 왼쪽이 원래 클라이언트다.
 */
object ClientIpResolver {

    private val HEADERS = listOf(
        "X-Forwarded-For",
        "X-Real-IP"
    )

    fun resolve(
        httpServletRequest: HttpServletRequest
    ): String {

        for (header in HEADERS) {

            val value = httpServletRequest.getHeader(header)

            if (!value.isNullOrBlank()) {

                return value.substringBefore(',').trim()
            }
        }

        return httpServletRequest.remoteAddr ?: "unknown"
    }
}
