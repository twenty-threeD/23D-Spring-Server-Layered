package spring.springserver.domain.notification.service.impl

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.data.response.ChatNotificationResponse
import spring.springserver.domain.notification.service.NotificationService
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationServiceImpl : NotificationService {

    private val emitters = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

    override fun subscribe(
        username: String
    ): SseEmitter {

        val emitter = SseEmitter(TIMEOUT_MILLIS)

        emitters.compute(username) { _, userEmitters ->
            (userEmitters ?: ConcurrentHashMap.newKeySet()).apply { add(emitter) }
        }

        emitter.onCompletion { remove(username, emitter) }
        emitter.onTimeout { remove(username, emitter) }
        emitter.onError { remove(username, emitter) }

        try {

            emitter.send(
                SseEmitter.event()
                    .name("connect")
                    .reconnectTime(RECONNECT_MILLIS)
                    .data("connected")
            )
        } catch (exception: Exception) {

            log.warn("SSE 최초 연결 이벤트 전송 실패: username={}", username, exception)

            remove(username, emitter)
            emitter.completeWithError(exception)
            throw exception
        }

        return emitter
    }

    override fun sendChatNotification(
        receiverUsername: String,
        notification: ChatNotificationResponse
    ) {

        dispatch(receiverUsername) {

            SseEmitter.event()
                .name("chat")
                .data(notification)
        }
    }

    /**
     * 프록시의 idle timeout으로 연결이 끊기거나 버퍼에 이벤트가 고이는 것을 막기 위해
     * 주기적으로 주석 이벤트를 흘려보낸다. 주석은 클라이언트 onmessage를 발생시키지 않는다.
     */
    @Scheduled(fixedRate = HEARTBEAT_MILLIS)
    fun sendHeartbeat() {

        emitters.keys.forEach {

            username ->
            dispatch(username) { SseEmitter.event().comment("ping") }
        }
    }

    private fun dispatch(
        username: String,
        event: () -> SseEmitter.SseEventBuilder
    ) {

        val userEmitters = emitters[username] ?: return
        val failed = mutableListOf<SseEmitter>()

        userEmitters.forEach {

            emitter ->
            try {

                emitter.send(event())
            } catch (exception: Exception) {

                log.debug("SSE 전송 실패로 emitter 정리: username={}", username, exception)

                failed += emitter
            }
        }

        failed.forEach {

            emitter ->
            remove(username, emitter)

            runCatching { emitter.complete() }
        }
    }

    private fun remove(
        username: String,
        emitter: SseEmitter
    ) {

        emitters.computeIfPresent(username) { _, userEmitters ->
            userEmitters.remove(emitter)
            userEmitters.takeIf { it.isNotEmpty() }
        }
    }

    companion object {

        private const val TIMEOUT_MILLIS = 5L * 60L * 1000L
        private const val RECONNECT_MILLIS = 3_000L
        private const val HEARTBEAT_MILLIS = 15_000L

        private val log = LoggerFactory.getLogger(NotificationServiceImpl::class.java)
    }
}
