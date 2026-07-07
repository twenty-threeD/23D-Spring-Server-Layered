package spring.springserver.global.config.websocket

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.global.jwt.TokenProvider

@Component
class WebSocketChannelInterceptor(
    private val tokenProvider: TokenProvider,
    private val chatService: ChatService,
) : ChannelInterceptor {

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel
    ): Message<*> {

        val accessor = StompHeaderAccessor.wrap(message)

        if (StompCommand.CONNECT == accessor.command && accessor.user == null) {

            var token = accessor.getFirstNativeHeader("Authorization")

            if (token != null && token.startsWith("Bearer ")) {

                token = token.substring(7)
            }

            if (token == null || tokenProvider.isNotValidToken(token)) {

                throw IllegalArgumentException("유효하지 않은 토큰")
            }

            val username = tokenProvider.getUsernameFromToken(token)
                ?: throw IllegalArgumentException("유효하지 않은 토큰")
            val parsedRole = tokenProvider.getRole(token)
            val role = parsedRole?.toString()

            accessor.user = StompPrincipal(
                username = username,
                role = role
            )
        }

        if (StompCommand.SUBSCRIBE == accessor.command) {

            validateRoomSubscription(accessor)
        }

        return message
    }

    private fun validateRoomSubscription(
        stompHeaderAccessor: StompHeaderAccessor
    ) {

        val user = stompHeaderAccessor.user
            ?: throw AccessDeniedException("인증되지 않은 사용자입니다.")

        val destination = stompHeaderAccessor.destination

        if (destination == null || !destination.startsWith(CHAT_ROOM_TOPIC_PREFIX)) {

            return
        }

        val roomId = extractRoomId(destination)

        if (!chatService.canAccessRoom(user.name, roomId)) {

            throw AccessDeniedException("해당 채팅방을 구독할 수 없습니다.")
        }
    }

    private fun extractRoomId(
        destination: String
    ): Long {

        val roomIdText = destination.substring(CHAT_ROOM_TOPIC_PREFIX.length)

        return roomIdText.toLongOrNull()
            ?: throw IllegalArgumentException("유효하지 않은 채팅방 구독 경로입니다.")
    }

    companion object {

        private const val CHAT_ROOM_TOPIC_PREFIX = "/topic/chat/rooms/"
    }
}
