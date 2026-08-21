package spring.springserver.domain.phone.data.response

data class PhoneVerifyResponse(
    val message: String
) {

    companion object {

        fun of(message: String): PhoneVerifyResponse {

            return PhoneVerifyResponse(message)
        }
    }
}
