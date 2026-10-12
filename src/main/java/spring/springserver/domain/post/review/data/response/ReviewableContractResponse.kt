package spring.springserver.domain.post.review.data.response

import spring.springserver.domain.contract.entity.Contract
import java.time.LocalDateTime

/**
 * 로그인 회원이 지금 리뷰를 남길 수 있는 계약 한 건.
 * 한 게시글에서 여러 전문가와 계약할 수 있어 어느 전문가와의 계약인지 함께 내려준다.
 */
data class ReviewableContractResponse(
    val contractId: Long?,

    val professionalId: Long?,

    val professionalName: String,

    val createdAt: LocalDateTime?
) {

    companion object {

        fun of(
            contract: Contract
        ): ReviewableContractResponse {

            return ReviewableContractResponse(
                contract.getId(),
                contract.professional.getId(),
                contract.professional.getDisplayName(),
                contract.getCreatedAt()
            )
        }
    }
}