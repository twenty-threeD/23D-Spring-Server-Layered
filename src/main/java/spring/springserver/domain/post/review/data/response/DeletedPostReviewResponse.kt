package spring.springserver.domain.post.review.data.response

data class DeletedPostReviewResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): DeletedPostReviewResponse {

            return DeletedPostReviewResponse(message)
        }
    }
}