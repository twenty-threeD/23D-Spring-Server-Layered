package spring.springserver.domain.auth.data.response

data class SignInResponse(
    val accessToken: String,

    val refreshToken: String
) {

    companion object {

        fun of(accessToken: String,
               refreshToken: String): SignInResponse {

            return SignInResponse(
                accessToken,
                refreshToken
            )
        }
    }
}