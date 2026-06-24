package spring.springserver.domain.member.data.response

data class ChangeUsernameResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): ChangeUsernameResponse {

            return ChangeUsernameResponse(message)
        }
    }
}