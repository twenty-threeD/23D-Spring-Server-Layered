package spring.springserver.domain.phone.data.response

data class SendPhoneVerifyNumberResponse(
    val message: String
) {

    companion object {

        fun of(message: String): SendPhoneVerifyNumberResponse {

            return SendPhoneVerifyNumberResponse(message)
        }
    }
}
