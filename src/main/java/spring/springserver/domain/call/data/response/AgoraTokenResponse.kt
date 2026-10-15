package spring.springserver.domain.call.data.response

import java.time.Instant

data class AgoraTokenResponse(
    val appId: String,
    val token: String,
    val expiresAt: Instant
) {

    companion object {

        fun of(
            appId: String,
            token: String,
            expiresAt: Instant
        ): AgoraTokenResponse =
            AgoraTokenResponse(
                appId = appId,
                token = token,
                expiresAt = expiresAt
            )
    }
}
