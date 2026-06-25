package spring.springserver.domain.member.data.response

data class CheckResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): CheckResponse {

            return CheckResponse(message)
        }
    }
}
