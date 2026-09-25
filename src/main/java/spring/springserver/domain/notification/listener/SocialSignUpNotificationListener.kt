package spring.springserver.domain.notification.listener

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.auth.event.SocialMemberSignedUpEvent
import spring.springserver.domain.notification.data.request.SendNoticeNotificationRequest
import spring.springserver.domain.notification.service.NotificationService

/**
 * 소셜 가입 회원은 비밀번호 없이 만들어지므로 가입 직후 한 번 설정을 안내한다.
 *
 * 알림 저장이 실패해도 가입은 살아 있어야 하므로 커밋 이후에 별도 스레드에서 보낸다.
 */
@Component
class SocialSignUpNotificationListener(
    private val notificationService: NotificationService
) {

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendPasswordNotSetNotification(
        event: SocialMemberSignedUpEvent
    ) {

        notificationService.sendNoticeNotification(
            SendNoticeNotificationRequest(
                event.username,
                PASSWORD_NOT_SET_MESSAGE
            )
        )
    }

    companion object {

        private const val PASSWORD_NOT_SET_MESSAGE = "비밀번호가 설정되지 않았어요. 프로필에서 비밀번호를 설정해주세요."
    }
}