package spring.springserver.domain.call.event

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.call.data.response.CallSignalResponse

/**
 * 통화 시그널을 양쪽 참여자의 개인 큐로 내보낸다.
 *
 * 커밋 이후에만 보낸다. 트랜잭션 안에서 보내면 롤백된 통화의 시그널이 상대에게 남아
 * 존재하지 않는 통화의 벨이 울리게 된다.
 * 시그널을 발생시킨 쪽에게도 보낸다. 같은 계정으로 다른 기기가 붙어 있을 수 있기 때문이다.
 */
@Component
class CallSignalEventListener(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    fun sendCallSignal(
        callSignalEvent: CallSignalEvent
    ) {

        val callSignalResponse = CallSignalResponse.of(
            signal = callSignalEvent.signal,
            call = callSignalEvent.call,
            fromUsername = callSignalEvent.fromUsername,
            occurredAt = callSignalEvent.occurredAt
        )

        val recipientUsernames = listOf(
            callSignalEvent.call.caller.username,
            callSignalEvent.call.callee.username
        ).distinct()

        recipientUsernames.forEach { username ->

            /**
             * 한쪽 전송이 실패해도 나머지 한쪽은 받아야 한다.
             * 여기서 예외가 올라가면 이미 커밋된 통화 상태와 시그널이 어긋난다.
             */
            runCatching {

                messagingTemplate.convertAndSendToUser(
                    username,
                    CALL_QUEUE_DESTINATION,
                    callSignalResponse
                )
            }.onFailure { throwable ->

                log.warn(
                    "통화 시그널 전송 실패 callId={}, signal={}, username={}",
                    callSignalEvent.call.callId,
                    callSignalEvent.signal,
                    username,
                    throwable
                )
            }
        }
    }

    companion object {

        const val CALL_QUEUE_DESTINATION = "/queue/call"
    }
}
