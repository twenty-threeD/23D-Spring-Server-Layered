package spring.springserver.domain.call.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.call.data.request.StartCallRequest
import spring.springserver.domain.call.data.request.UpdateCallMediaRequest
import spring.springserver.domain.call.data.response.CallResponse
import spring.springserver.domain.call.data.response.CallSessionResponse
import spring.springserver.domain.call.data.response.ScreenShareTokenResponse
import spring.springserver.domain.call.service.CallMediaService
import spring.springserver.domain.call.service.CallService
import spring.springserver.global.data.BaseResponse
import java.security.Principal

@RestController
@RequestMapping("/api/call")
class CallController(
    private val callService: CallService,
    private val callMediaService: CallMediaService
) {

    @PostMapping
    fun startCall(
        @RequestBody @Valid startCallRequest: StartCallRequest,
        principal: Principal
    ): BaseResponse<CallSessionResponse> =
        BaseResponse.ok(
            callService.startCall(
                callerUsername = principal.name,
                startCallRequest = startCallRequest
            )
        )

    @PostMapping("/{callId}/accept")
    fun acceptCall(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallSessionResponse> =
        BaseResponse.ok(
            callService.acceptCall(
                username = principal.name,
                callId = callId
            )
        )

    @PostMapping("/{callId}/reject")
    fun rejectCall(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callService.rejectCall(
                username = principal.name,
                callId = callId
            )
        )

    @PostMapping("/{callId}/cancel")
    fun cancelCall(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callService.cancelCall(
                username = principal.name,
                callId = callId
            )
        )

    @PostMapping("/{callId}/end")
    fun endCall(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callService.endCall(
                username = principal.name,
                callId = callId
            )
        )

    /**
     * 토큰 만료가 다가올 때 클라이언트가 호출해 새 토큰을 받아간다.
     * 미디어 전환에는 필요 없다. 토큰에 음성/영상 권한이 모두 들어 있기 때문이다.
     */
    @PostMapping("/{callId}/token")
    fun renewToken(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallSessionResponse> =
        BaseResponse.ok(
            callService.renewToken(
                username = principal.name,
                callId = callId
            )
        )

    /**
     * 통화 중 마이크/카메라 전환. 음성 통화에서 카메라를 켜는 것도 여기로 보낸다.
     */
    @PatchMapping("/{callId}/media")
    fun updateMedia(
        @RequestBody @Valid updateCallMediaRequest: UpdateCallMediaRequest,
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callMediaService.updateMedia(
                username = principal.name,
                callId = callId,
                updateCallMediaRequest = updateCallMediaRequest
            )
        )

    @PostMapping("/{callId}/screen-share")
    fun startScreenShare(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<ScreenShareTokenResponse> =
        BaseResponse.ok(
            callMediaService.startScreenShare(
                username = principal.name,
                callId = callId
            )
        )

    @DeleteMapping("/{callId}/screen-share")
    fun stopScreenShare(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callMediaService.stopScreenShare(
                username = principal.name,
                callId = callId
            )
        )

    @GetMapping("/rooms/{roomId}")
    fun getRoomCalls(
        @PathVariable roomId: Long,
        principal: Principal
    ): BaseResponse<List<CallResponse>> =
        BaseResponse.ok(
            callService.getRoomCalls(
                username = principal.name,
                roomId = roomId
            )
        )

    @GetMapping("/{callId}")
    fun getCall(
        @PathVariable callId: Long,
        principal: Principal
    ): BaseResponse<CallResponse> =
        BaseResponse.ok(
            callService.getCall(
                username = principal.name,
                callId = callId
            )
        )
}
