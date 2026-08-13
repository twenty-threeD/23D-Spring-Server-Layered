package spring.springserver.domain.notification.service

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import spring.springserver.domain.notification.data.request.SendNoticeNotificationRequest
import spring.springserver.domain.notification.data.response.ChatNotificationResponse
import spring.springserver.domain.notification.data.response.DeletedNotificationResponse
import spring.springserver.domain.notification.data.response.NotificationResponse
import spring.springserver.domain.notification.data.response.UnreadNotificationCountResponse

interface NotificationService {

    fun subscribe(
        username: String,
        lastEventId: Long?
    ): SseEmitter

    fun sendChatNotification(
        receiverUsername: String,
        notification: ChatNotificationResponse
    ): NotificationResponse

    fun sendNoticeNotification(
        sendNoticeNotificationRequest: SendNoticeNotificationRequest
    ): NotificationResponse

    fun getNotifications(
        username: String
    ): List<NotificationResponse>

    fun getUnreadCount(
        username: String
    ): UnreadNotificationCountResponse

    /**
     * 읽은 알림은 보관하지 않으므로 읽음 처리는 곧 삭제다.
     */
    fun readNotification(
        username: String,
        notificationId: Long
    ): DeletedNotificationResponse

    fun readAllNotifications(
        username: String
    ): DeletedNotificationResponse
}