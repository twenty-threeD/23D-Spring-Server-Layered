package spring.springserver.domain.member.data.response

data class DeleteAccountResponse(
    val message: String
) {

    companion object {

        fun of(message: String): DeleteAccountResponse {

            return DeleteAccountResponse(message)
        }
    }
}