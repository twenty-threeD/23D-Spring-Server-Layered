package spring.springserver.domain.notification.listener

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.entity.CallType
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.notification.service.NotificationService

/**
 * 통화가 부재중으로 끝나면 받는 사람에게 알림을 남긴다.
 *
 * 통화 시그널은 STOMP로만 가서 접속해 있지 않으면 놓친다. 알림은 DB에 남고
 * SSE 재접속 시 다시 흘려보내지므로 나중에 들어와도 부재중 전화를 알 수 있다.
 * 통화 정리가 커밋된 뒤에만 보낸다. 알림 실패가 통화 정리를 되돌리면 안 되기 때문이다.
 */
@Component
class MissedCallNotificationListener(
    private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    fun sendMissedCallNotification(
        callSignalEvent: CallSignalEvent
    ) {

        if (callSignalEvent.signal != CallSignalType.MISSED) return

        val call = callSignalEvent.call
        val callTypeLabel = if (call.callType == CallType.VIDEO) "영상" else "음성"

        runCatching {

            notificationService.sendMissedCallNotification(
                receiverUsername = call.callee.username,
                callerUsername = call.caller.username,
                message = "${call.caller.name}님의 부재중 $callTypeLabel 통화가 있습니다.",
                roomId = call.roomId
            )
        }.onFailure { exception ->

            log.warn("부재중 통화 알림을 보내지 못했습니다. callId = {}", call.callId, exception)
        }
    }
}
