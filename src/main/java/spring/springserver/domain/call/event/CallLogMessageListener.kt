package spring.springserver.domain.call.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.chat.data.response.ChatCallLogResponse
import spring.springserver.domain.chat.service.ChatService

/**
 * 통화가 끝나면 그 채팅방에 통화 기록 메시지를 남긴다.
 *
 * 클라이언트가 남기면 발신자가 창을 바로 닫거나 연결이 끊겼을 때 기록이 빠진다.
 * 통화 상태가 확정된(커밋된) 뒤에만 남기고, 기록 실패가 통화 처리를 되돌리지 않게 한다.
 */
@Component
class CallLogMessageListener(
    private val chatService: ChatService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    fun recordCallLog(
        callSignalEvent: CallSignalEvent
    ) {

        if (callSignalEvent.signal !in FINISHED_SIGNALS) return

        val call = callSignalEvent.call
        val roomId = call.roomId
            ?: return

        runCatching {

            chatService.sendCallLogMessage(
                roomId = roomId,
                senderUsername = call.caller.username,
                callLog = ChatCallLogResponse.of(
                    callId = call.callId,
                    callType = call.callType,
                    status = call.status,
                    durationSeconds = call.durationSeconds,
                    callerUsername = call.caller.username
                )
            )
        }.onFailure { exception ->

            log.warn("통화 기록 메시지를 남기지 못했습니다. callId = {}", call.callId, exception)
        }
    }

    companion object {

        private val FINISHED_SIGNALS = setOf(
            CallSignalType.ENDED,
            CallSignalType.MISSED,
            CallSignalType.REJECTED,
            CallSignalType.CANCELED
        )
    }
}
