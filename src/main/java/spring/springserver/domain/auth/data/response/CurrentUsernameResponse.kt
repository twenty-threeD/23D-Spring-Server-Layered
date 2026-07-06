package spring.springserver.domain.auth.data.response

data class CurrentUsernameResponse(

    val username: String
) {

    companion object {

        fun of(
            username: String
        ): CurrentUsernameResponse {

            return CurrentUsernameResponse(username)
        }
    }
}
