package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract
import java.time.LocalDateTime

data class CreateContractResponse(
    val id: Long?,

    /**
     * 갑(의뢰인)의 회원 아이디. 대금을 지급하는 쪽이다.
     */
    val clientId: Long?,

    /**
     * 을(전문가)의 회원 아이디. 용역을 제공하고 대금을 받는 쪽이다.
     */
    val professionalId: Long?,

    /**
     * 계약서를 등록한 당사자의 회원 아이디.
     */
    val writerId: Long?,

    val startedAt: LocalDateTime?,

    val endedAt: LocalDateTime?,

    /**
     * 검수 기간(일).
     */
    val inspectionPeriod: Int,

    /**
     * 계약 금액(원).
     */
    val price: Long,

    val servicesDescription: String
) {

    companion object {

        fun of(
            contract: Contract
        ): CreateContractResponse {

            return CreateContractResponse(
                contract.getId(),
                contract.client.getId(),
                contract.professional.getId(),
                contract.writer.getId(),
                contract.startedAt,
                contract.endedAt,
                contract.inspectionPeriod,
                contract.price,
                contract.servicesDescription
            )
        }
    }
}
