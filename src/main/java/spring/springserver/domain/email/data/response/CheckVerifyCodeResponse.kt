package spring.springserver.domain.email.data.response

data class CheckVerifyCodeResponse(

    val message: String
) {

    companion object {

        fun of(message: String): CheckVerifyCodeResponse {

            return CheckVerifyCodeResponse(message)
        }
    }
}
