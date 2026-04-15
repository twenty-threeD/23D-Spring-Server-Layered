package spring.springserver.global.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import spring.springserver.global.config.websocket.StompPrincipal;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        String username = Objects.toString(attributes.get("username"), null);
        String role = Objects.toString(attributes.get("role"), null);

        if (username != null && role != null) {
            log.info("Principal 생성 - 사용자: {}, 권한: {}", username, role);
            return new StompPrincipal(username, role);
        }

        log.warn("토큰 검증 실패: username={}, role={}", username, role);
        return null;
    }
}