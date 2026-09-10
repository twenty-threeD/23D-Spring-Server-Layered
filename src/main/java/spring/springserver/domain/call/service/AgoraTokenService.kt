package spring.springserver.domain.call.service

import spring.springserver.domain.call.data.response.AgoraTokenResponse

interface AgoraTokenService {

    /**
     * Agora RTC 채널 접속용 토큰을 발급한다.
     *
     * 음성/영상/데이터 송출 권한을 모두 담는다.
     * 음성 통화라고 영상 권한을 빼면 통화 중 카메라를 켤 때마다 토큰을 다시 받아야 하고,
     * 그 사이 몇 프레임이 유실된다. 무엇을 켜고 끌지는 미디어 상태로 관리한다.
     */
    fun issueRtcToken(
        channelName: String,
        uid: Int
    ): AgoraTokenResponse
}
