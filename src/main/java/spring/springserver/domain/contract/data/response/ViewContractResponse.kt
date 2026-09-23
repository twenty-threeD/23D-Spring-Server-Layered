package spring.springserver.domain.contract.data.response

import spring.springserver.domain.contract.entity.Contract
import java.time.LocalDateTime

/**
 * 계약서 한 건의 전체 내용.
 *
 * 예전에는 계약서가 PDF 경로 한 줄이어서 그것만 돌려줬지만,
 * 이제 계약 조건이 테이블에 들어오므로 화면이 그릴 수 있도록 전부 내려준다.
 */
data class ViewContractResponse(
    val id: Long?,
    val clientId: Long?,
    val clientName: String,
    val professionalId: Long?,
    val professionalName: String,
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

    val servicesDescription: String,

    /**
     * 계약서 PDF 경로.
     */
    val contractUrl: String,

    val createdAt: LocalDateTime?,

    /**
     * 계약의 출발점이 된 게시글 아이디. 게시글 없이 맺은 계약이면 null이다.
     */
    val postId: Long?
) {

    companion object {

        fun of(
            contract: Contract
        ): ViewContractResponse {

            return ViewContractResponse(
                contract.getId(),
                contract.client.getId(),
                contract.client.name,
                contract.professional.getId(),
                contract.professional.name,
                contract.writer.getId(),
                contract.startedAt,
                contract.endedAt,
                contract.inspectionPeriod,
                contract.price,
                contract.servicesDescription,
                contract.contractUrl,
                contract.getCreatedAt(),
                contract.post?.getId()
            )
        }
    }
}
