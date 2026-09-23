package spring.springserver.domain.contract.data.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateContractRequest(
    /**
     * 갑(의뢰인)의 회원 아이디. 용역을 의뢰하고 대금을 지급하는 쪽이다.
     */
    @field:NotNull(message = "의뢰인(갑) 회원 아이디는 필수입니다.")
    val clientId: Long?,

    /**
     * 을(전문가)의 회원 아이디. 용역을 제공하고 대금을 받는 쪽이다.
     */
    @field:NotNull(message = "전문가(을) 회원 아이디는 필수입니다.")
    val professionalId: Long?,

    /**
     * 용역 시작 시각. 아직 정하지 않았으면 보내지 않아도 된다.
     */
    val startedAt: LocalDateTime?,

    /**
     * 용역 종료 시각. 아직 정하지 않았으면 보내지 않아도 된다.
     */
    val endedAt: LocalDateTime?,

    /**
     * 검수 기간(일). 검수 없이 바로 마감하는 계약이면 0을 보낸다.
     */
    @field:NotNull(message = "검수 기간은 필수입니다.")
    @field:PositiveOrZero(message = "검수 기간은 0일 이상이어야 합니다.")
    val inspectionPeriod: Int?,

    /**
     * 계약 금액(원). 의뢰인(갑)이 전문가(을)에게 지급하기로 한 용역 대금이다.
     */
    @field:NotNull(message = "계약 금액은 필수입니다.")
    @field:Min(value = 1, message = "계약 금액은 1원 이상이어야 합니다.")
    val price: Long?,

    /**
     * 계약 대상 용역의 내용.
     */
    @field:NotBlank(message = "용역 내용은 필수입니다.")
    @field:Size(max = 2000, message = "용역 내용은 2000자 이하로 입력해주세요.")
    val servicesDescription: String?,

    /**
     * 파일 업로드 API가 돌려준 계약서 PDF의 경로.
     */
    @field:NotBlank(message = "계약서 URL은 필수입니다.")
    @field:Size(max = 2048, message = "계약서 URL은 2048자 이하로 입력해주세요.")
    val contractUrl: String?,

    /**
     * 계약의 출발점이 된 게시글 아이디. 채팅에서 바로 맺은 계약이면 보내지 않아도 된다.
     * 보내면 해당 게시글의 리뷰 목록에 이 계약의 리뷰가 함께 묶인다.
     */
    @field:Positive(message = "게시글 아이디는 1 이상이어야 합니다.")
    val postId: Long? = null
)
