package spring.springserver.domain.call.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.call.repository.CallRepository
import spring.springserver.domain.call.service.CallMaintenanceService
import spring.springserver.global.config.AgoraProperties
import java.time.Instant

@Service
@Transactional(rollbackFor = [Exception::class])
class CallMaintenanceServiceImpl(
    private val callRepository: CallRepository,
    private val agoraProperties: AgoraProperties,
    private val eventPublisher: ApplicationEventPublisher
): CallMaintenanceService {

    override fun expireStaleCalls() {

        val now = Instant.now()

        /**
         * 아무도 받지 않은 통화. 벨이 영원히 울리지 않도록 부재중으로 닫는다.
         */
        val missedCalls = callRepository.findAllByStatusAndCreatedAtBefore(
            status = CallStatus.RINGING,
            threshold = now.minusSeconds(agoraProperties.ringingTimeoutSeconds)
        )

        for (call in missedCalls) {

            call.miss(now)

            publish(
                call = call,
                signal = CallSignalType.MISSED
            )
        }

        /**
         * 종료 요청 없이 방치된 통화. 두 사람이 새 통화를 걸 수 있게 강제로 닫는다.
         */
        val abandonedCalls = callRepository.findAllByStatusAndStartedAtBefore(
            status = CallStatus.ACCEPTED,
            threshold = now.minusSeconds(agoraProperties.maxCallDurationSeconds)
        )

        for (call in abandonedCalls) {

            call.end(
                at = now,
                memberId = null
            )

            publish(
                call = call,
                signal = CallSignalType.ENDED
            )
        }
    }

    private fun publish(
        call: Call,
        signal: CallSignalType
    ) {

        eventPublisher.publishEvent(
            CallSignalEvent.of(
                signal = signal,
                call = CallResponse.of(call),
                fromUsername = null
            )
        )
    }
}
