package spring.springserver.global.config.websocket

import org.springframework.context.annotation.Lazy
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.global.jwt.TokenProvider

@Component
class WebSocketChannelInterceptor(
    private val tokenProvider: TokenProvider,
    private val tokenService: TokenService,
    @Lazy private val chatService: ChatService,
) : ChannelInterceptor {

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel
    ): Message<*> {

        val accessor = StompHeaderAccessor.wrap(message)

        if (StompCommand.CONNECT == accessor.command && accessor.user == null) {

            val token = accessor.getFirstNativeHeader("Authorization")
                ?.removePrefix("Bearer ")
                ?: throw IllegalArgumentException("유효하지 않은 토큰")

            /**
             * 서명만 보면 refreshToken, 로그아웃한 토큰, 탈퇴 회원의 토큰도 통과하므로
             * HTTP 요청과 같은 기준으로 확인한다.
             */
            val username = tokenService.resolveActiveUsername(token)
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
