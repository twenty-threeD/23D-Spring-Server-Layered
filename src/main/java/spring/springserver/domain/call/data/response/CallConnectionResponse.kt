package spring.springserver.domain.call.data.response

import java.time.Instant

/**
 * Agora 채널 접속 정보. 클라이언트는 이 값으로 join 한다.
 */
data class CallConnectionResponse(
    val appId: String,
    val channelName: String,
    val uid: Int,
    val rtcToken: String,
    val tokenExpiresAt: Instant
) {

    companion object {

        fun of(
            channelName: String,
            uid: Int,
            agoraTokenResponse: AgoraTokenResponse
        ): CallConnectionResponse =
            CallConnectionResponse(
                appId = agoraTokenResponse.appId,
                channelName = channelName,
                uid = uid,
                rtcToken = agoraTokenResponse.token,
                tokenExpiresAt = agoraTokenResponse.expiresAt
            )
    }
}
