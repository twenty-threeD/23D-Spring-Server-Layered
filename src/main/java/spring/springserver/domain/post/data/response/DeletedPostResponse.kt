package spring.springserver.domain.post.data.response

import spring.springserver.domain.auth.data.response.SignOutResponse

data class DeletedPostResponse(

    val message: String
) {

    companion object {

        fun of(message: String): DeletedPostResponse {

            return DeletedPostResponse(message)
        }
    }
}
