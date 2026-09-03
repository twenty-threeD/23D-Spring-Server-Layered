package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract

data class ViewContractResponse(
    val contractUrl: String
) {

    companion object {

        fun of(
            contract: Contract
        ): ViewContractResponse {

            return ViewContractResponse(
                contract.contractUrl
            )
        }
    }
}
