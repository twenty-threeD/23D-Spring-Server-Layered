package spring.springserver.domain.member.data.response

data class UsernameCheckResponse(
    val available: Boolean,

    val message: String
) {

    companion object {

        fun of(
            available: Boolean,
            message: String
        ): UsernameCheckResponse {

            return UsernameCheckResponse(available, message)
        }
    }
}