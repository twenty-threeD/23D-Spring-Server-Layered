package spring.springserver.domain.member.data.response

data class UsernameCheckResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): UsernameCheckResponse {

            return UsernameCheckResponse(message)
        }
    }
}