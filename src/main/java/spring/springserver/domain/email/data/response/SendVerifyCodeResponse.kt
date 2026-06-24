package spring.springserver.domain.email.data.response

data class SendVerifyCodeResponse(

    var message: String
) {

    companion object {

        fun of(message: String): SendVerifyCodeResponse {

            return SendVerifyCodeResponse(message)
        }
    }
}
