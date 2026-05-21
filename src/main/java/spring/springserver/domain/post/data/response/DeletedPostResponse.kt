package spring.springserver.domain.post.data.response

data class DeletedPostResponse(

    val message: String
) {

    companion object {

        fun of(message: String): DeletedPostResponse {

            return DeletedPostResponse(message)
        }
    }
}
