package spring.springserver.global.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import spring.springserver.global.jwt.JwtProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("STOMP CONNECT 요청");

            // Header에서 JWT 토큰 추출
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰 검증
            if (token == null || jwtProvider.isValidToken(token)) {
                log.error("유효하지 않은 토큰");
                throw new IllegalArgumentException("유효하지 않은 토큰");
            }

            // JWT에서 사용자 정보 추출
            String username = jwtProvider.getUsernameFromToken(token);
            String role = jwtProvider.getRole(token).toString();

            // Principal 설정
            accessor.setUser(new StompPrincipal(username, role));

            log.info("사용자 {} (권한: {}) 연결됨", username, role);
        }

        return message;
    }
}