package spring.springserver.domain.push.listener

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import spring.springserver.domain.chat.event.ChatMessageSentEvent
import spring.springserver.domain.chat.service.ChatService
import spring.springserver.domain.push.service.PushSendService

/**
 * 채팅 메시지를 방의 상대방 기기로 푸시한다.
 * Expo 호출이 메시지 전송 스레드를 붙잡지 않게 알림 실행기에서 돈다.
 * 지금 보고 있는 방인지는 앱이 거르므로 서버는 항상 보낸다.
 */
@Component
class ChatPushListener(
    private val chatService: ChatService,
    private val pushSendService: PushSendService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("notificationExecutor")
    @EventListener
    fun sendChatPush(
        chatMessageSentEvent: ChatMessageSentEvent
    ) {

        val message = chatMessageSentEvent.message
        val roomId = message.roomId
            ?: return

        val body = ChatPushPreview.of(message)
            ?: return

        runCatching {

            val receiver = chatService.getOtherParticipantUsername(
                roomId = roomId,
                username = chatMessageSentEvent.senderUsername
            )

            if (receiver.username == chatMessageSentEvent.senderUsername) return

            pushSendService.sendToUsername(
                username = receiver.username,
                title = message.senderName,
                body = body,
                data = mapOf(
                    "type" to "chat",
                    "roomId" to roomId
                )
            )
        }.onFailure { throwable ->

            log.warn("채팅 푸시를 보내지 못했습니다. roomId = {}", roomId, throwable)
        }
    }
}
