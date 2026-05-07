package spring.springserver.domain.oauth.data.response

/**
 * @author @gnlandkmg(개발)
 * @author @L98293(코틀린 변환)
 */
data class OAuthResponse(

    val accessToken: String,

    val refreshToken: String
) {

    companion object {

        fun of(
            accessToken: String,
            refreshToken: String
        ): OAuthResponse {

            return OAuthResponse(
                accessToken,
                refreshToken
            )
        }
    }
}