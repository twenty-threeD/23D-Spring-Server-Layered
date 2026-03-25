package spring.springserver.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            // 메시지 브로커 설정
            // /topic으로 시작하는 경로는 브로드캐스트 -> 여러명한테 보낼때
            // /queue로 시작하는 경로는 개인 메시지 -> 서버에서 메세지를 보내는거
            config.enableSimpleBroker("/topic", "/queue");

            // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix
            config.setApplicationDestinationPrefixes("/app");

            // 사용자별 메시지 prefix -> 갠메(유저가 보냄)
            config.setUserDestinationPrefix("/user");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            // WebSocket 연결 엔드포인트
            registry.addEndpoint("/ws-stomp")
                    .setAllowedOrigins("*")  // CORS 설정
                    .withSockJS();           // SockJS: WebSocket미지원 브라우저도 사용가능하게 만들어줌
        }
}
