package spring.springserver.domain.notification.data.response

import spring.springserver.domain.notification.entity.Notification
import spring.springserver.domain.notification.entity.NotificationType
import java.time.Instant

data class NotificationResponse(
    val notificationId: Long?,
    val type: NotificationType,
    val senderUsername: String?,
    val senderName: String?,
    val receiverUsername: String,
    val message: String,
    val sentAt: Instant,
    val roomId: Long?
) {

    companion object {

        fun of(
            notification: Notification
        ): NotificationResponse =
            NotificationResponse(
                notificationId = notification.getId(),
                type = notification.type,
                senderUsername = notification.sender?.username,
                senderName = notification.sender?.name,
                receiverUsername = notification.receiver.username,
                message = notification.message,
                sentAt = notification.sentAt,
                roomId = notification.roomId
            )
    }
}