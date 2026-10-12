package spring.springserver.global.handler

import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

/**
 * WebSocket 연결의 사용자는 핸드셰이크 HTTP 요청에서 JWT 필터가 이미 검증한 로그인 사용자로 정한다.
 *
 * 예전에는 `?token=` 쿼리나 헤더의 토큰을 서명만 보고 그대로 썼다. 그러면 본인 토큰으로 핸드셰이크를
 * 통과한 뒤 쿼리에 남의 토큰(로그아웃·탈퇴 회원·refreshToken 포함)을 넣어 그 사람으로 접속할 수 있었다.
 */
@Component
class CustomHandshakeHandler: DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal? {

        return request.principal
    }
}