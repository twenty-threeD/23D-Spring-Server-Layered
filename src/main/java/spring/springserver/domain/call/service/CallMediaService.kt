package spring.springserver.domain.call.service

import spring.springserver.domain.call.data.request.UpdateCallMediaRequest
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.ScreenShareTokenResponse

/**
 * 통화 중 송출 상태 전환.
 *
 * 마이크/카메라는 토큰 재발급 없이 상태만 바꾸면 되고,
 * 화면 공유는 Agora가 별도 uid 접속을 요구하므로 전용 토큰을 발급한다.
 */
interface CallMediaService {

    fun updateMedia(
        username: String,
        callId: Long,
        updateCallMediaRequest: UpdateCallMediaRequest
    ): CallResponse

    fun startScreenShare(
        username: String,
        callId: Long
    ): ScreenShareTokenResponse

    fun stopScreenShare(
        username: String,
        callId: Long
    ): CallResponse
}
