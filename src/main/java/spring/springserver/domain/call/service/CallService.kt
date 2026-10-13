package spring.springserver.domain.call.service

import spring.springserver.domain.call.data.request.StartCallRequest
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSessionResponse

/**
 * 통화 라이프사이클. 걸기 · 받기 · 끊기와 조회를 담당한다.
 * 통화 중 미디어 전환은 [CallMediaService]가 맡는다.
 */
interface CallService {

    fun startCall(
        callerUsername: String,
        startCallRequest: StartCallRequest
    ): CallSessionResponse

    fun acceptCall(
        username: String,
        callId: Long
    ): CallSessionResponse

    fun rejectCall(
        username: String,
        callId: Long
    ): CallResponse

    fun cancelCall(
        username: String,
        callId: Long
    ): CallResponse

    fun endCall(
        username: String,
        callId: Long
    ): CallResponse

    fun renewToken(
        username: String,
        callId: Long
    ): CallSessionResponse

    fun getCall(
        username: String,
        callId: Long
    ): CallResponse

    fun getRoomCalls(
        username: String,
        roomId: Long
    ): List<CallResponse>

    /**
     * 내가 참여 중인 진행 중 통화와 다시 접속할 토큰. 없으면 null이다.
     * 새로고침이나 재접속 후 통화를 복구할 때 쓴다.
     */
    fun getActiveCall(
        username: String
    ): CallSessionResponse?

    /**
     * 연결이 끊긴 채 돌아오지 않은 회원의 진행 중 통화를 정리한다.
     * 연결된 통화는 종료, 아직 받지 않은 통화를 건 쪽이면 취소한다.
     * 받는 쪽이 끊긴 RINGING 통화는 부재중 처리에 맡긴다.
     */
    fun endCallsOnDisconnect(
        username: String
    )
}
