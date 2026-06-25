package spring.springserver.global.handler

import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.util.UriComponentsBuilder
import spring.springserver.global.config.websocket.StompPrincipal
import spring.springserver.global.jwt.TokenProvider
import java.security.Principal

@Component
class CustomHandshakeHandler(
    private val tokenProvider: TokenProvider
) : DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal? {

        val token = extractToken(request)

        if (token.isNullOrBlank()) {

            return null
        }

        if (tokenProvider.isNotValidToken(token)) {

            return null
        }

        val username = tokenProvider.getUsernameFromToken(token)

        if (username.isNullOrBlank()) {

            return null
        }

        val role = tokenProvider.getRole(token)

        return StompPrincipal(
            username = username,
            role = role?.name
        )
    }

    private fun extractToken(
        request: ServerHttpRequest
    ): String? {

        val queryParams = UriComponentsBuilder.fromUri(request.uri)
            .build()
            .queryParams

        val queryToken = queryParams.getFirst("token")

        if (!queryToken.isNullOrBlank()) {

            return queryToken
        }

        val authorizationHeader = request.headers.getFirst("Authorization")

        return authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
    }
}
