package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.contract.entity.ContractStatus
import java.time.LocalDateTime

data class ContractResponse(
    val id: Long?,

    /**
     * 계약서 PDF 경로. 채팅 메시지에 첨부해 그대로 보여준다.
     */
    val contractUrl: String,

    val partA: Long?,

    val partB: Long?,

    val amount: Long,

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

    val partASigned: Boolean,

    val partBSigned: Boolean,

    val partASignedAt: LocalDateTime?,

    val partBSignedAt: LocalDateTime?,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            contract: Contract
        ): ContractResponse {

            return ContractResponse(
                contract.getId(),
                contract.contractUrl,
                contract.partA.getId(),
                contract.partB.getId(),
                contract.amount,
                contract.writer.getId(),
                contract.getStatus(),
                contract.isSigned(),
                contract.isPartASigned(),
                contract.isPartBSigned(),
                contract.getPartASignedAt(),
                contract.getPartBSignedAt(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
            )
        }
    }
}
