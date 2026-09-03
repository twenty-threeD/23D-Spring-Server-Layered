package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.contract.entity.ContractStatus
import java.time.LocalDateTime

data class CreateContractResponse(
    val id: Long?,

    /**
     * 계약서 PDF 경로. 채팅 메시지에 첨부해 그대로 보여준다.
     */
    val contractUrl: String,

    /**
     * 갑(의뢰인)의 회원 아이디. 대금을 지급하는 쪽이다.
     */
    val clientId: Long?,

    /**
     * 을(전문가)의 회원 아이디. 용역을 제공하고 대금을 받는 쪽이다.
     */
    val professionalId: Long?,

    /**
     * 계약 금액(원). 의뢰인(갑)이 전문가(을)에게 지급하기로 한 용역 대금이다.
     */
    val price: Long,

    /**
     * 계약서를 등록한 당사자의 회원 아이디.
     */
    val writerId: Long?,

    /**
     * 양측 서명이 모두 모이기 전이면 DRAFT, 모이면 SIGNED.
     */
    val status: ContractStatus,

    /**
     * status에서 파생되는 값이다. 계약 성립 여부만 필요한 화면을 위해 함께 내려준다.
     */
    val signed: Boolean,

    val clientSigned: Boolean,

    val professionalSigned: Boolean,

    val clientSignedAt: LocalDateTime?,

    val professionalSignedAt: LocalDateTime?,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            contract: Contract
        ): CreateContractResponse {

            return CreateContractResponse(
                contract.getId(),
                contract.contractUrl,
                contract.client.getId(),
                contract.professional.getId(),
                contract.price,
                contract.writer.getId(),
                contract.getStatus(),
                contract.isSigned(),
                contract.isClientSigned(),
                contract.isProfessionalSigned(),
                contract.getClientSignedAt(),
                contract.getProfessionalSignedAt(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
            )
        }
    }
}
