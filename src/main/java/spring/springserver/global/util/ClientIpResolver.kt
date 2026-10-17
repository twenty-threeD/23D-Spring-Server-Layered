package spring.springserver.global.util

import jakarta.servlet.http.HttpServletRequest

/**
 * nginx 뒤에서 돈다. X-Real-IP는 nginx가 $remote_addr로 덮어쓰므로 가장 믿을 수 있다.
 * X-Forwarded-For는 클라이언트가 보낸 값 뒤에 nginx가 덧붙이므로 맨 왼쪽은 위조할 수 있다.
 * 그래서 맨 오른쪽(마지막 프록시가 본 주소)을 쓴다.
 * remoteAddr도 forward-headers-strategy=framework 때문에 XFF 맨 왼쪽으로 바뀌어 있어 마지막 수단으로만 쓴다.
 */
object ClientIpResolver {

    fun resolve(
        httpServletRequest: HttpServletRequest
    ): String {

        val realIp = httpServletRequest.getHeader("X-Real-IP")

        if (!realIp.isNullOrBlank()) {

            return realIp.trim()
        }

        val forwardedFor = httpServletRequest.getHeader("X-Forwarded-For")

        if (!forwardedFor.isNullOrBlank()) {

            return forwardedFor.substringAfterLast(',').trim()
        }

        return httpServletRequest.remoteAddr ?: "unknown"
    }
}