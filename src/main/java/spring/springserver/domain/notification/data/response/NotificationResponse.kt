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
    val roomId: Long?,
    val postId: Long?
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
                roomId = notification.roomId,
                postId = notification.postId
            )

        /**
         * 수신자를 배치로 조회한 팬아웃 경로용. 그 경로의 `receiver`는 프록시라
         * `of`처럼 username을 읽으면 수신자마다 조회가 한 번 더 나간다.
         *
         * 보내는 사람이 있는 알림이 이 경로로 들어오면 sender를 null로 박지 않고
         * `of`에 넘긴다. 조회 한 번을 아끼려다 조용히 틀린 값을 내보내지 않기 위해서다.
         */
        fun ofFanout(
            notification: Notification,
            receiverUsername: String
        ): NotificationResponse {

            if (notification.sender != null) {

                return of(notification)
            }

            return NotificationResponse(
                notificationId = notification.getId(),
                type = notification.type,
                senderUsername = null,
                senderName = null,
                receiverUsername = receiverUsername,
                message = notification.message,
                sentAt = notification.sentAt,
                roomId = notification.roomId,
                postId = notification.postId
            )
        }
    }
}