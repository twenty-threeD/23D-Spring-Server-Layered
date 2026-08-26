package spring.springserver.domain.member.data.response

data class ChangePhoneResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): ChangePhoneResponse {

            return ChangePhoneResponse(message)
        }
    }
}
