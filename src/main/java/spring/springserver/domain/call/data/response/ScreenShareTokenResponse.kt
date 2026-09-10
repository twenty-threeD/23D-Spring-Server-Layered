package spring.springserver.domain.call.data.response

import spring.springserver.domain.call.entity.Call
import java.time.Instant

/**
 * 화면 공유용 접속 정보.
 *
 * Agora는 화면 공유를 카메라와 별도의 uid로 같은 채널에 한 번 더 접속시키는 방식이라
 * 카메라 토큰과는 다른 토큰이 필요하다.
 */
data class ScreenShareTokenResponse(
    val call: CallResponse,
    val appId: String,
    val channelName: String,
    val screenUid: Int,
    val rtcToken: String,
    val tokenExpiresAt: Instant
) {

    companion object {

        fun of(
            call: Call,
            memberId: Long?,
            agoraTokenResponse: AgoraTokenResponse
        ): ScreenShareTokenResponse =
            ScreenShareTokenResponse(
                call = CallResponse.of(call),
                appId = agoraTokenResponse.appId,
                channelName = call.channelName,
                screenUid = call.screenUidOf(memberId),
                rtcToken = agoraTokenResponse.token,
                tokenExpiresAt = agoraTokenResponse.expiresAt
            )
    }
}
