package spring.springserver.domain.member.data.response

data class PasswordResetResponse(
    val message: String
) {

    companion object {

        fun of(message: String): PasswordResetResponse {

            return PasswordResetResponse(message)
        }
    }
}