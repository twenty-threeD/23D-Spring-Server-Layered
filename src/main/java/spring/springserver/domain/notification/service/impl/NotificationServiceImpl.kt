package spring.springserver.domain.notification.service.impl

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.notification.data.request.SendNoticeNotificationRequest
import spring.springserver.domain.notification.data.response.ChatNotificationResponse
import spring.springserver.domain.notification.data.response.DeletedNotificationResponse
import spring.springserver.domain.notification.data.response.NotificationResponse
import spring.springserver.domain.notification.data.response.UnreadNotificationCountResponse
import spring.springserver.domain.notification.entity.Notification
import spring.springserver.domain.notification.entity.NotificationType
import spring.springserver.domain.notification.exception.NotificationStatusCode
import spring.springserver.domain.notification.repository.NotificationRepository
import spring.springserver.domain.notification.service.NotificationService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val memberRepository: MemberRepository
) : NotificationService {

    private val emitters = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

    override fun subscribe(
        username: String,
        lastEventId: Long?
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

        replayMissedNotifications(username, emitter, lastEventId)

        return emitter
    }

    /**
     * 오프라인이었던 동안 쌓인 알림은 SSE로 전달되지 못했으므로,
     * 재접속 시 DB에 저장해 둔 미수신 알림을 그대로 다시 흘려보낸다.
     */
    private fun replayMissedNotifications(
        username: String,
        emitter: SseEmitter,
        lastEventId: Long?
    ) {

        // 재전송에 실패하더라도 연결 자체는 유지한다.
        // 여기서 emitter를 끊으면 클라이언트가 곧바로 재접속해 조회를 반복하고,
        // 그 과정에서 커넥션 풀이 더 빠르게 고갈된다.
        try {

            notificationRepository.findMissedByReceiverUsername(
                username = username,
                lastEventId = lastEventId ?: 0L
            ).forEach {

                notification ->
                emitter.send(event(NotificationResponse.of(notification)))
            }
        } catch (exception: Exception) {

            log.warn("놓친 알림 재전송 실패: username={}", username, exception)
        }
    }

    @Transactional
    override fun sendChatNotification(
        receiverUsername: String,
        notification: ChatNotificationResponse
    ): NotificationResponse {

        val response = save(
            type = NotificationType.CHAT,
            receiverUsername = receiverUsername,
            senderUsername = notification.senderUsername,
            message = notification.message,
            sentAt = notification.createdAt,
            roomId = notification.roomId
        )

        dispatch(receiverUsername) { event(response) }

        return response
    }

    @Transactional
    override fun sendNoticeNotification(
        sendNoticeNotificationRequest: SendNoticeNotificationRequest
    ): NotificationResponse {

        val response = save(
            type = NotificationType.NOTICE,
            receiverUsername = sendNoticeNotificationRequest.receiverUsername,
            senderUsername = null,
            message = sendNoticeNotificationRequest.message,
            sentAt = Instant.now(),
            roomId = null
        )

        dispatch(sendNoticeNotificationRequest.receiverUsername) { event(response) }

        return response
    }

    @Transactional(readOnly = true)
    override fun getNotifications(
        username: String
    ): List<NotificationResponse> {

        return notificationRepository.findAllByReceiverUsername(username)
            .map { NotificationResponse.of(it) }
    }

    @Transactional(readOnly = true)
    override fun getUnreadCount(
        username: String
    ): UnreadNotificationCountResponse {

        return UnreadNotificationCountResponse(notificationRepository.countByReceiverUsername(username))
    }

    @Transactional
    override fun readNotification(
        username: String,
        notificationId: Long
    ): DeletedNotificationResponse {

        val notification = notificationRepository.findById(notificationId).orElse(null)
            ?: throw ApplicationException.of(NotificationStatusCode.NOTIFICATION_NOT_FOUND)

        if (notification.receiver.username != username) {

            throw ApplicationException.of(NotificationStatusCode.FORBIDDEN_NOTIFICATION_ACCESS)
        }

        notificationRepository.delete(notification)

        return DeletedNotificationResponse(1)
    }

    @Transactional
    override fun readAllNotifications(
        username: String
    ): DeletedNotificationResponse {

        return DeletedNotificationResponse(notificationRepository.deleteAllByReceiverUsername(username))
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

    private fun save(
        type: NotificationType,
        receiverUsername: String,
        senderUsername: String?,
        message: String,
        sentAt: Instant,
        roomId: Long?
    ): NotificationResponse {

        val receiver = memberRepository.findByUsername(receiverUsername)
            ?: throw ApplicationException.of(MemberStatusCode.MEMBER_NOT_FOUND)

        val sender = senderUsername?.let {

            memberRepository.findByUsername(it)
                ?: throw ApplicationException.of(MemberStatusCode.MEMBER_NOT_FOUND)
        }

        val notification = notificationRepository.save(
            Notification(
                type = type,
                receiver = receiver,
                message = message,
                sentAt = sentAt,
                sender = sender,
                roomId = roomId
            )
        )

        return NotificationResponse.of(notification)
    }

    /**
     * 이벤트 id로 알림 id를 실어 보낸다. 브라우저 EventSource가 재연결할 때
     * 이 값을 Last-Event-ID 헤더로 되돌려주므로 놓친 알림의 기준점이 된다.
     */
    private fun event(
        notificationResponse: NotificationResponse
    ): SseEmitter.SseEventBuilder =
        SseEmitter.event()
            .id(notificationResponse.notificationId.toString())
            .name(notificationResponse.type.name.lowercase())
            .data(notificationResponse)

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

        private const val TIMEOUT_MILLIS = 30L * 60L * 1000L
        private const val RECONNECT_MILLIS = 3_000L
        private const val HEARTBEAT_MILLIS = 15_000L

        private val log = LoggerFactory.getLogger(NotificationServiceImpl::class.java)
    }
}