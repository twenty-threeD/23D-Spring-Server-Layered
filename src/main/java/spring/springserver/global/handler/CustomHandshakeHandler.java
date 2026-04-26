package spring.springserver.global.handler;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;
import spring.springserver.domain.member.entity.Role;
import spring.springserver.global.config.websocket.StompPrincipal;
import spring.springserver.global.jwt.JwtProvider;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtProvider jwtProvider;

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest serverHttpRequest,
                                      @NonNull WebSocketHandler webSocketHandler,
                                      @NonNull Map<String, Object> attributes) {

        String token = extractToken(serverHttpRequest);
        if (token == null || token.isBlank()) {
            return null;
        }

        if (jwtProvider.isValidToken(token)) {
            return null;
        }

        String username = jwtProvider.getUsernameFromToken(token);
        Role role = jwtProvider.getRole(token);

        if (username == null || username.isBlank()) {
            return null;
        }

        return new StompPrincipal(username, role != null ? role.name() : null);
    }

    private String extractToken(ServerHttpRequest request) {

        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = queryParams.getFirst("token");
        if (token != null && !token.isBlank()) {
            return token;
        }

        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        return null;
    }
}
