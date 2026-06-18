package spring.springserver.domain.auth.data.response

data class SignUpResponse(
    val message: String
) {

    companion object {

        fun of(message: String): SignUpResponse {

            return SignUpResponse(message)
        }
    }
}