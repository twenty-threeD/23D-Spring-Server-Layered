package spring.springserver.domain.profile.data.response

data class UpdateProfileResponse(
    val message: String
) {

    companion object {

        fun of(message: String): UpdateProfileResponse {

            return UpdateProfileResponse(message)
        }
    }
}