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

    /**
     * 구인/구직 글이 올라왔을 때 조건에 맞는 회원들에게 한 번에 알린다.
     * 받는 사람이 여러 명이므로 개별 발송이 실패해도 나머지는 계속 보낸다.
     */
    fun sendJobPostNotification(
        receiverUsernames: Collection<String>,
        message: String,
        postId: Long
    ): List<NotificationResponse>

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