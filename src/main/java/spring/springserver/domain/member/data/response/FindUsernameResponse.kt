package spring.springserver.domain.member.data.response

data class FindUsernameResponse(
    val username: String
) {

    companion object {

        fun of(
            username: String
        ): FindUsernameResponse {

            return FindUsernameResponse(username)
        }
    }
}