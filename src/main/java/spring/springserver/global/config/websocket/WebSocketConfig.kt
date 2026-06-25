package spring.springserver.global.config.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import spring.springserver.global.handler.CustomHandshakeHandler

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketChannelInterceptor: WebSocketChannelInterceptor,
    private val customHandshakeHandler: CustomHandshakeHandler,
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(
        messageBrokerRegistry: MessageBrokerRegistry
    ) {

        messageBrokerRegistry.enableSimpleBroker(
            "/topic",
            "/queue"
        )
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app")
        messageBrokerRegistry.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(
        stompEndpointRegistry: StompEndpointRegistry
    ) {

        stompEndpointRegistry.addEndpoint("/ws-stomp")
            .setAllowedOriginPatterns("*")
            .setHandshakeHandler(customHandshakeHandler)
            .withSockJS()
    }

    override fun configureClientInboundChannel(
        channelRegistration: ChannelRegistration
    ) {

        channelRegistration.interceptors(webSocketChannelInterceptor)
    }
}
