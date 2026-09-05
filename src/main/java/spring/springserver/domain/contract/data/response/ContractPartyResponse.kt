package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract

data class ContractPartyResponse(
    val clientId: Long?,
    val clientName: String,
    val professionalId: Long?,
    val professionalName: String
) {

    companion object {

        fun of(
            contract: Contract
        ): ContractPartyResponse {

            return ContractPartyResponse(
                clientId = contract.client.getId(),
                clientName = contract.client.name,
                professionalId = contract.professional.getId(),
                professionalName = contract.professional.name
            )
        }
    }
}
