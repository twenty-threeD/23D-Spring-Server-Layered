package spring.springserver.domain.call.data.response

import spring.springserver.domain.call.entity.Call
import java.time.Instant

/**
 * Agora 채널에 접속하기 위해 필요한 정보 묶음.
 *
 * 클라이언트는 `appId`로 엔진을 만들고 `channelName`/`uid`/`rtcToken`으로 join 한다.
 * 토큰에는 음성과 영상 송출 권한이 모두 들어 있으므로, 통화 중에 카메라를 켜고 끄는 데
 * 토큰을 다시 받을 필요는 없다. 상태만 미디어 갱신 API로 알리면 된다.
 */
data class CallSessionResponse(
    val call: CallResponse,
    val appId: String,
    val channelName: String,
    val uid: Int,
    val rtcToken: String,
    val tokenExpiresAt: Instant
) {

    companion object {

        fun of(
            call: Call,
            memberId: Long?,
            agoraTokenResponse: AgoraTokenResponse
        ): CallSessionResponse =
            CallSessionResponse(
                call = CallResponse.of(call),
                appId = agoraTokenResponse.appId,
                channelName = call.channelName,
                uid = call.cameraUidOf(memberId),
                rtcToken = agoraTokenResponse.token,
                tokenExpiresAt = agoraTokenResponse.expiresAt
            )
    }
}
