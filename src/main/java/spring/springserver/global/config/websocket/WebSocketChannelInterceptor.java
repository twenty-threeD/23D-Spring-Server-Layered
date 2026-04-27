package spring.springserver.global.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import spring.springserver.domain.chat.service.ChatService;
import spring.springserver.global.jwt.JwtProvider;

@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel messageChannel) {

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

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateRoomSubscription(accessor);
        }

        return message;
    }

    private void validateRoomSubscription(StompHeaderAccessor accessor) {

        if (accessor.getUser() == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith("/topic/chat/rooms/")) {
            return;
        }

        Long roomId = extractRoomId(destination);

        if (!chatService.canAccessRoom(accessor.getUser().getName(), roomId)) {
            throw new AccessDeniedException("해당 채팅방을 구독할 수 없습니다.");
        }
    }

    private Long extractRoomId(String destination) {

        String roomIdText = destination.substring("/topic/chat/rooms/".length());

        try {
            return Long.parseLong(roomIdText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 채팅방 구독 경로입니다.");
        }
    }
}
