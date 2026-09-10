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
}
