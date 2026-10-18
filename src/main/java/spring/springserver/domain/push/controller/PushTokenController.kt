package spring.springserver.domain.push.controller

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.push.data.request.RegisterPushTokenRequest
import spring.springserver.domain.push.data.request.UnregisterPushTokenRequest
import spring.springserver.domain.push.service.PushTokenService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.jwt.MemberDetails

@RestController
@RequestMapping("/api/push")
class PushTokenController(
    private val pushTokenService: PushTokenService
) {

    @PostMapping("/token")
    fun registerToken(
        @Valid @RequestBody registerPushTokenRequest: RegisterPushTokenRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<Void> {

        pushTokenService.registerToken(
            registerPushTokenRequest,
            memberDetails.getId()
        )

        return BaseResponse.ok(null)
    }

    @DeleteMapping("/token")
    fun unregisterToken(
        @Valid @RequestBody unregisterPushTokenRequest: UnregisterPushTokenRequest,
        @AuthenticationPrincipal memberDetails: MemberDetails
    ): BaseResponse<Void> {

        pushTokenService.unregisterToken(
            unregisterPushTokenRequest,
            memberDetails.getId()
        )

        return BaseResponse.ok(null)
    }
}
