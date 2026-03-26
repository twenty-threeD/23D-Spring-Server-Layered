package spring.springserver.global.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import spring.springserver.global.jwt.JwtProvider;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class HttpSessionIdHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        String token = null;

        // 1. URL 쿼리 파라미터에서 토큰 추출 (?token=xxx)
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            token = query.split("token=")[1];
            if (token.contains("&")) {
                token = token.split("&")[0];
            }
        }

        // 2. 토큰이 없으면 Header에서 찾기 (Authorization: Bearer xxx)
        if (token == null) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 3. 토큰이 없으면 Cookie에서 찾기
        if (token == null && request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            token = jwtProvider.resolveToken(servletRequest);
        }

        // 4. 토큰 검증
        if (token != null && !jwtProvider.isValidToken(token)) {
            String username = jwtProvider.getUsernameFromToken(token);
            String role = jwtProvider.getRole(token).toString();

            attributes.put("username", username);
            attributes.put("role", role);
            attributes.put("token", token);

            log.info("토큰 검증 성공 - 사용자: {}, 권한: {}", username, role);
            return true;
        }

        log.error("토큰 검증 실패");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}