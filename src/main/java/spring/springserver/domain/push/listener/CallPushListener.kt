package spring.springserver.domain.push.listener

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.entity.CallType
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.push.service.PushSendService

/**
 * 통화가 걸리거나(INVITED) 부재중(MISSED)이 되면 받는 사람 기기로 푸시한다.
 * 롤백된 통화의 벨이 울리지 않게 커밋 이후에만 보낸다.
 */
@Component
class CallPushListener(
    private val pushSendService: PushSendService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("notificationExecutor")
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    fun sendCallPush(
        callSignalEvent: CallSignalEvent
    ) {

        val call = callSignalEvent.call

        val (body, data) = when (callSignalEvent.signal) {

            CallSignalType.INVITED -> {

                val label = if (call.callType == CallType.VIDEO) "영상" else "음성"

                "$label 통화가 왔어요" to mapOf(
                    "type" to "call",
                    "callId" to call.callId,
                    "roomId" to call.roomId,
                    "callType" to call.callType.name
                )
            }

            CallSignalType.MISSED -> "부재중 전화가 있어요" to mapOf(
                "type" to "call",
                "callId" to call.callId,
                "roomId" to call.roomId
            )

            else -> return
        }

        if (call.callee.username == call.caller.username) return

        runCatching {

            pushSendService.sendToUsername(
                username = call.callee.username,
                title = call.caller.name,
                body = body,
                data = data,
                highPriority = true,
                channelId = CALL_CHANNEL_ID
            )
        }.onFailure { throwable ->

            log.warn("통화 푸시를 보내지 못했습니다. callId = {}, signal = {}", call.callId, callSignalEvent.signal, throwable)
        }
    }

    companion object {

        private const val CALL_CHANNEL_ID = "call"
    }
}
