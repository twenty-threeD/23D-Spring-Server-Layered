package spring.springserver.domain.member.data.response

data class UsernameCheckResponse(
    val available: Boolean,

    val message: String
) {

    companion object {

        fun of(available: Boolean): UsernameCheckResponse {

            return UsernameCheckResponse(
                available,
                if (available) "사용 가능한 사용자명입니다." else "이미 사용 중인 사용자명입니다."
            )
        }
    }
}