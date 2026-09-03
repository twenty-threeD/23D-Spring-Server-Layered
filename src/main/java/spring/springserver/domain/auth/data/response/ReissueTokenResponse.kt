package spring.springserver.domain.auth.data.response

data class ReissueTokenResponse(
    val accessToken: String
) {

    companion object {

        fun of(
            accessToken: String
        ): ReissueTokenResponse {

            return ReissueTokenResponse(accessToken)
        }
    }
}
