package spring.springserver.domain.call.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.call.data.request.StartCallRequest
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSessionResponse
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.entity.CallStatus
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.call.exception.CallStatusCode
import spring.springserver.domain.call.repository.CallRepository
import spring.springserver.domain.call.service.AgoraTokenService
import spring.springserver.domain.call.service.CallService
import spring.springserver.domain.call.support.CallAccessSupport
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

@Service
@Transactional(rollbackFor = [Exception::class])
class CallServiceImpl(
    private val callRepository: CallRepository,
    private val callAccessSupport: CallAccessSupport,
    private val agoraTokenService: AgoraTokenService,
    private val eventPublisher: ApplicationEventPublisher
): CallService {

    override fun startCall(
        callerUsername: String,
        startCallRequest: StartCallRequest
    ): CallSessionResponse {

        val roomId = startCallRequest.roomId
            ?: throw ApplicationException(CallStatusCode.CALL_ROOM_NOT_FOUND)
        val callType = startCallRequest.callType
            ?: throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT)

        val caller = callAccessSupport.getMemberByUsername(callerUsername)
        val room = callAccessSupport.getRoom(roomId)

        if (!callAccessSupport.isRoomParticipant(room, caller.getId())) {

            throw ApplicationException(CallStatusCode.CALL_ROOM_FORBIDDEN)
        }

        val callee = callAccessSupport.getRoomCounterpart(room, caller.getId())

        if (callee.getId() == caller.getId()) {

            throw ApplicationException(CallStatusCode.CALL_SELF_NOT_ALLOWED)
        }

        ensureNoActiveCall(
            callerId = caller.getId()!!,
            calleeId = callee.getId()!!
        )

        val uids = generateChannelUids()

        val call = callRepository.save(
            Call(
                room = room,
                caller = caller,
                callee = callee,
                channelName = generateChannelName(),
                callType = callType,
                callerUid = uids[0],
                calleeUid = uids[1],
                callerScreenUid = uids[2],
                calleeScreenUid = uids[3]
            )
        )

        return toSessionResponse(
            call = call,
            memberId = caller.getId(),
            signal = CallSignalType.INVITED,
            fromUsername = callerUsername
        )
    }

    override fun acceptCall(
        username: String,
        callId: Long
    ): CallSessionResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        if (call.isCaller(member.getId())) {

            throw ApplicationException.of(
                CallStatusCode.CALL_FORBIDDEN,
                "발신자는 통화를 수락할 수 없습니다."
            )
        }

        call.accept(Instant.now())

        return toSessionResponse(
            call = call,
            memberId = member.getId(),
            signal = CallSignalType.ACCEPTED,
            fromUsername = username
        )
    }

    override fun rejectCall(
        username: String,
        callId: Long
    ): CallResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        if (call.isCaller(member.getId())) {

            throw ApplicationException.of(
                CallStatusCode.CALL_FORBIDDEN,
                "발신자는 통화를 거절할 수 없습니다."
            )
        }

        call.reject(
            at = Instant.now(),
            memberId = member.getId()
        )

        return publish(
            call = call,
            signal = CallSignalType.REJECTED,
            fromUsername = username
        )
    }

    override fun cancelCall(
        username: String,
        callId: Long
    ): CallResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        if (!call.isCaller(member.getId())) {

            throw ApplicationException.of(
                CallStatusCode.CALL_FORBIDDEN,
                "수신자는 통화를 취소할 수 없습니다."
            )
        }

        call.cancel(
            at = Instant.now(),
            memberId = member.getId()
        )

        return publish(
            call = call,
            signal = CallSignalType.CANCELED,
            fromUsername = username
        )
    }

    /**
     * 어느 쪽이든 통화를 끊는다.
     *
     * 아직 연결 전(RINGING)에 들어오면 발신자는 취소, 수신자는 거절로 처리한다.
     * 끊기 버튼과 수락 사이의 경합에서 409가 나지 않도록 상태에 맞춰 흡수한다.
     */
    override fun endCall(
        username: String,
        callId: Long
    ): CallResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )
        val now = Instant.now()
        val isCaller = call.isCaller(member.getId())

        val signal = when (call.status) {

            CallStatus.RINGING -> if (isCaller) {

                call.cancel(
                    at = now,
                    memberId = member.getId()
                )

                CallSignalType.CANCELED
            } else {

                call.reject(
                    at = now,
                    memberId = member.getId()
                )

                CallSignalType.REJECTED
            }

            CallStatus.ACCEPTED -> {

                call.end(
                    at = now,
                    memberId = member.getId()
                )

                CallSignalType.ENDED
            }

            else -> throw ApplicationException(CallStatusCode.CALL_INVALID_STATUS)
        }

        return publish(
            call = call,
            signal = signal,
            fromUsername = username
        )
    }

    /**
     * 토큰 만료가 다가오면 클라이언트가 호출해 새 토큰을 받아간다.
     * 시그널을 보내지 않는다. 상대에게는 아무 변화도 아니기 때문이다.
     */
    @Transactional(readOnly = true)
    override fun renewToken(
        username: String,
        callId: Long
    ): CallSessionResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        if (call.status.isFinished()) {

            throw ApplicationException(CallStatusCode.CALL_INVALID_STATUS)
        }

        return CallSessionResponse.of(
            call = call,
            memberId = member.getId(),
            agoraTokenResponse = agoraTokenService.issueRtcToken(
                channelName = call.channelName,
                uid = call.cameraUidOf(member.getId())
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getCall(
        username: String,
        callId: Long
    ): CallResponse {

        val member = callAccessSupport.getMemberByUsername(username)

        return CallResponse.of(
            callAccessSupport.getParticipatingCall(
                callId = callId,
                memberId = member.getId()
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getRoomCalls(
        username: String,
        roomId: Long
    ): List<CallResponse> {

        val member = callAccessSupport.getMemberByUsername(username)
        val room = callAccessSupport.getRoom(roomId)

        if (!callAccessSupport.isRoomParticipant(room, member.getId())) {

            throw ApplicationException(CallStatusCode.CALL_ROOM_FORBIDDEN)
        }

        return callRepository.findAllByRoomId(roomId)
            .map(CallResponse::of)
    }

    private fun toSessionResponse(
        call: Call,
        memberId: Long?,
        signal: CallSignalType,
        fromUsername: String
    ): CallSessionResponse {

        val agoraTokenResponse = agoraTokenService.issueRtcToken(
            channelName = call.channelName,
            uid = call.cameraUidOf(memberId)
        )

        publish(
            call = call,
            signal = signal,
            fromUsername = fromUsername
        )

        return CallSessionResponse.of(
            call = call,
            memberId = memberId,
            agoraTokenResponse = agoraTokenResponse
        )
    }

    /**
     * 트랜잭션 안에서 스냅샷을 뜨고, 실제 전송은 커밋 이후로 미룬다.
     */
    private fun publish(
        call: Call,
        signal: CallSignalType,
        fromUsername: String?
    ): CallResponse {

        val callResponse = CallResponse.of(call)

        eventPublisher.publishEvent(
            CallSignalEvent.of(
                signal = signal,
                call = callResponse,
                fromUsername = fromUsername
            )
        )

        return callResponse
    }

    private fun ensureNoActiveCall(
        callerId: Long,
        calleeId: Long
    ) {

        val hasActiveCall = callRepository.existsByStatusInAndParticipantIn(
            statuses = listOf(CallStatus.RINGING, CallStatus.ACCEPTED),
            memberIds = listOf(callerId, calleeId)
        )

        if (hasActiveCall) {

            throw ApplicationException(CallStatusCode.CALL_ALREADY_IN_PROGRESS)
        }
    }

    private fun generateChannelName(): String =
        "call-" + UUID.randomUUID().toString().replace("-", "")

    /**
     * 카메라 2개 + 화면 공유 2개, 총 4개의 uid를 뽑는다.
     *
     * Agora uid는 채널 안에서만 유일하면 되므로 통화마다 새로 뽑아도 충돌하지 않는다.
     * 0은 Agora가 "서버가 배정"으로 해석하므로 쓰지 않는다.
     */
    private fun generateChannelUids(): List<Int> {

        val uids = LinkedHashSet<Int>()

        while (uids.size < UID_COUNT) {

            uids.add(Random.nextInt(1, Int.MAX_VALUE))
        }

        return uids.toList()
    }

    companion object {

        private const val UID_COUNT = 4
    }
}
