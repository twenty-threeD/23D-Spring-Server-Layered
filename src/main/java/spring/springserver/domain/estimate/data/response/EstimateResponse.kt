package spring.springserver.domain.estimate.data.response

import spring.springserver.domain.estimate.entity.Estimate
import java.time.LocalDateTime

data class EstimateResponse(
    val id: Long?,

    val clientId: Long?,

    val professionalId: Long?,

    val url: String,

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
                estimate.client.getId(),
                estimate.professional.getId(),
                estimate.url,
                estimate.paid,
                estimate.getCreatedAt(),
                estimate.getUpdatedAt()
            )
        }
    }
}
