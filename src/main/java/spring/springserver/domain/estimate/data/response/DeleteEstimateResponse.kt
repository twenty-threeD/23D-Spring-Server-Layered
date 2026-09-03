package spring.springserver.domain.estimate.data.response

data class DeleteEstimateResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): DeleteEstimateResponse {

            return DeleteEstimateResponse(message)
        }
    }
}
