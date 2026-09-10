package spring.springserver.domain.call.service.impl

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.call.data.request.UpdateCallMediaRequest
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSignalType
import spring.springserver.domain.call.data.response.ScreenShareTokenResponse
import spring.springserver.domain.call.entity.Call
import spring.springserver.domain.call.event.CallSignalEvent
import spring.springserver.domain.call.service.AgoraTokenService
import spring.springserver.domain.call.service.CallMediaService
import spring.springserver.domain.call.support.CallAccessSupport
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.exception.status_code.CommonStatusCode

@Service
@Transactional(rollbackFor = [Exception::class])
class CallMediaServiceImpl(
    private val callAccessSupport: CallAccessSupport,
    private val agoraTokenService: AgoraTokenService,
    private val eventPublisher: ApplicationEventPublisher
): CallMediaService {

    /**
     * 마이크/카메라 상태만 바꾼다. 토큰에는 이미 두 권한이 다 들어 있으므로 재발급하지 않는다.
     * 음성으로 시작한 통화에서 카메라를 켜는 것도 이 경로다.
     */
    override fun updateMedia(
        username: String,
        callId: Long,
        updateCallMediaRequest: UpdateCallMediaRequest
    ): CallResponse {

        val audioEnabled = updateCallMediaRequest.audioEnabled
            ?: throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT)
        val videoEnabled = updateCallMediaRequest.videoEnabled
            ?: throw ApplicationException.of(CommonStatusCode.INVALID_ARGUMENT)

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        call.updateMedia(
            memberId = member.getId(),
            audioEnabled = audioEnabled,
            videoEnabled = videoEnabled
        )

        return publish(
            call = call,
            signal = CallSignalType.MEDIA_CHANGED,
            fromUsername = username
        )
    }

    override fun startScreenShare(
        username: String,
        callId: Long
    ): ScreenShareTokenResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        call.startScreenShare(member.getId()!!)

        val agoraTokenResponse = agoraTokenService.issueRtcToken(
            channelName = call.channelName,
            uid = call.screenUidOf(member.getId())
        )

        publish(
            call = call,
            signal = CallSignalType.SCREEN_SHARE_STARTED,
            fromUsername = username
        )

        return ScreenShareTokenResponse.of(
            call = call,
            memberId = member.getId(),
            agoraTokenResponse = agoraTokenResponse
        )
    }

    override fun stopScreenShare(
        username: String,
        callId: Long
    ): CallResponse {

        val member = callAccessSupport.getMemberByUsername(username)
        val call = callAccessSupport.getParticipatingCall(
            callId = callId,
            memberId = member.getId()
        )

        call.stopScreenShare(member.getId()!!)

        return publish(
            call = call,
            signal = CallSignalType.SCREEN_SHARE_STOPPED,
            fromUsername = username
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
}
