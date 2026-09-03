package spring.springserver.domain.estimate.data.response

import spring.springserver.domain.estimate.entity.Estimate
import spring.springserver.domain.estimate.entity.EstimateStatus
import java.time.LocalDateTime

data class EstimateResponse(
    val id: Long?,

    val postId: Long?,

    val clientId: Long?,

    val professionalId: Long?,

    val url: String,

    val totalPay: Long,

    /**
     * 결제 전이면 PROPOSED, 결제가 끝났으면 PAID.
     */
    val status: EstimateStatus,

    /**
     * status에서 파생되는 값이다. 결제 여부만 필요한 화면을 위해 함께 내려준다.
     */
    val paid: Boolean,

    val createdAt: LocalDateTime?,

    val updatedAt: LocalDateTime?
) {

    companion object {

        fun of(
            estimate: Estimate
        ): EstimateResponse {

            return EstimateResponse(
                estimate.getId(),
                estimate.post.getId(),
                estimate.client.getId(),
                estimate.professional.getId(),
                estimate.url,
                estimate.totalPay,
                estimate.getStatus(),
                estimate.isPaid(),
                estimate.getCreatedAt(),
                estimate.getUpdatedAt()
            )
        }
    }
}
