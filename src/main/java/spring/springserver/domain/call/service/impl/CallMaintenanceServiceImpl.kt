package spring.springserver.domain.call.service.impl

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.call.repository.CallRepository
import spring.springserver.domain.call.service.CallMaintenanceService
import spring.springserver.domain.call.support.CallAccessSupport
import spring.springserver.global.config.AgoraProperties
import java.time.Instant

@Service
class CallMaintenanceServiceImpl(
    private val callRepository: CallRepository,
    private val agoraProperties: AgoraProperties,
    private val eventPublisher: ApplicationEventPublisher,
    private val callAccessSupport: CallAccessSupport,
    transactionManager: PlatformTransactionManager
): CallMaintenanceService {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 통화 한 건마다 트랜잭션을 따로 연다. 한 트랜잭션으로 묶으면 한 건의 실패(예: 그사이 수락된 통화)가
     * 전체를 롤백시키고, 다음 주기에도 같은 건에서 계속 막힌다.
     */
    private val perCallTransaction = TransactionTemplate(transactionManager).apply {

        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun expireStaleCalls() {

        val now = Instant.now()
        val ringingThreshold = now.minusSeconds(agoraProperties.ringingTimeoutSeconds)
        val durationThreshold = now.minusSeconds(agoraProperties.maxCallDurationSeconds)

        /**
         * 아무도 받지 않은 통화. 벨이 영원히 울리지 않도록 부재중으로 닫는다.
         */
        callRepository.findAllByStatusAndCreatedAtBefore(
            status = CallStatus.RINGING,
            threshold = ringingThreshold
        ).forEach { call ->

            processLocked(call.getId()!!) { locked ->

                /**
                 * 목록을 읽은 뒤 수락·취소됐을 수 있으므로 잠근 뒤 다시 확인한다.
                 * 그사이 수락된 통화를 부재중으로 덮어쓰면 통화 중인데 부재중 기록이 남는다.
                 */
                if (locked.status != CallStatus.RINGING) return@processLocked

                locked.miss(now)

                publish(
                    call = locked,
                    signal = CallSignalType.MISSED
                )
            }
        }

        /**
         * 종료 요청 없이 방치된 통화. 두 사람이 새 통화를 걸 수 있게 강제로 닫는다.
         */
        callRepository.findAllByStatusAndStartedAtBefore(
            status = CallStatus.ACCEPTED,
            threshold = durationThreshold
        ).forEach { call ->

            processLocked(call.getId()!!) { locked ->

                if (locked.status != CallStatus.ACCEPTED) return@processLocked

                locked.end(
                    at = now,
                    memberId = null
                )

                publish(
                    call = locked,
                    signal = CallSignalType.ENDED
                )
            }
        }
    }

    private fun processLocked(
        callId: Long,
        action: (Call) -> Unit
    ) {

        runCatching {

            perCallTransaction.executeWithoutResult {

                callRepository.findByIdForUpdate(callId)?.let(action)
            }
        }.onFailure { exception ->

            log.warn("방치된 통화를 정리하지 못했습니다. 다음 주기에 다시 시도합니다. callId = {}", callId, exception)
        }
    }

    private fun publish(
        call: Call,
        signal: CallSignalType
    ) {

        eventPublisher.publishEvent(
            CallSignalEvent.of(
                signal = signal,
                call = CallResponse.of(
                    call,
                    callAccessSupport.getImageUrls(call)
                ),
                fromUsername = null
            )
        )
    }
}
