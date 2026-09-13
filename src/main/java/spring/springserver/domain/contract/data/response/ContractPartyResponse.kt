package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract

/**
 * 계약 당사자만 추린 응답. 결제·원장 검증에서 요청자가 당사자인지 확인하는 데 쓴다.
 */
data class ContractPartyResponse(
    val clientId: Long?,
    val clientName: String,
    val professionalId: Long?,
    val professionalName: String,
    val contractUrl: String
) {

    companion object {

        fun of(
            contract: Contract
        ): ContractPartyResponse {

            return ContractPartyResponse(
                clientId = contract.client.getId(),
                clientName = contract.client.name,
                professionalId = contract.professional.getId(),
                professionalName = contract.professional.name,
                contractUrl = contract.contractUrl
            )
        }
    }
}
