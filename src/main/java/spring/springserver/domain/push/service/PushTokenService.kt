package spring.springserver.domain.push.service

import spring.springserver.domain.push.data.request.RegisterPushTokenRequest
import spring.springserver.domain.push.data.request.UnregisterPushTokenRequest

interface PushTokenService {

    fun registerToken(
        registerPushTokenRequest: RegisterPushTokenRequest,
        memberId: Long?
    )

    fun unregisterToken(
        unregisterPushTokenRequest: UnregisterPushTokenRequest,
        memberId: Long?
    )
}
