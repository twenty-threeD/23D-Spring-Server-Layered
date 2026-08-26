package spring.springserver.domain.contract.data.response

data class DeleteContractResponse(
    val message: String
) {

    companion object {

        fun of(
            message: String
        ): DeleteContractResponse {

            return DeleteContractResponse(message)
        }
    }
}
