package spring.springserver.domain.member.data.response

data class ChangeEmailResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): ChangeEmailResponse {

            return ChangeEmailResponse(message)
        }
    }
}
