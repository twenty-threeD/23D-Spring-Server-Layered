package spring.springserver.domain.auth.data.response

data class SignOutResponse(
    val message: String
) {

    companion object {

        fun of(message: String): SignOutResponse {

            return SignOutResponse(message)
        }
    }
}