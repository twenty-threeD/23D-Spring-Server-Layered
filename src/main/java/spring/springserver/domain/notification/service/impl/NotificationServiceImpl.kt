package spring.springserver.domain.notification.service.impl

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.data.response.ChatNotificationResponse
import spring.springserver.domain.notification.service.NotificationService
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationServiceImpl : NotificationService {

    private val emitters = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

    override fun subscribe(
        username: String
    ): SseEmitter {

        val emitter = SseEmitter(TIMEOUT_MILLIS)
        emitters.computeIfAbsent(username) { ConcurrentHashMap.newKeySet() }.add(emitter)

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
        } catch (exception: IOException) {

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

        val userEmitters = emitters[receiverUsername] ?: return
        val failed = mutableListOf<SseEmitter>()

        userEmitters.forEach {

            emitter ->
            try {

                emitter.send(
                    SseEmitter.event()
                        .name("chat")
                        .data(notification)
                )
            } catch (_: IOException) {

                failed += emitter
            } catch (_: IllegalStateException) {

                failed += emitter
            }
        }

        failed.forEach { remove(receiverUsername, it) }
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
    }
}
