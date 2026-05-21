package spring.springserver.domain.community.global.data.response

data class DeleteResponse(
    val message: String
) {

    companion object {

        fun of(message: String): DeleteResponse {

            return DeleteResponse(message);
        }
    }
}
