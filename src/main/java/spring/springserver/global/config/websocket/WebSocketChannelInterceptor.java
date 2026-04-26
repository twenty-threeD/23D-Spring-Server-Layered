package spring.springserver.global.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import spring.springserver.global.jwt.JwtProvider;

@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getUser() == null) {

            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token == null || jwtProvider.isValidToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰");
            }

            String username = jwtProvider.getUsernameFromToken(token);
            var parsedRole = jwtProvider.getRole(token);
            String role = parsedRole != null ? parsedRole.toString() : null;

            accessor.setUser(new StompPrincipal(username, role));
        }

        return message;
    }
}
