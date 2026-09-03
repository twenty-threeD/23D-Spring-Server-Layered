package spring.springserver.domain.contract.data.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateContractRequest(
    @field:NotBlank(message = "계약서 URL은 필수입니다.")
    @field:Size(max = 2048, message = "계약서 URL은 2048자 이하로 입력해주세요.")
    val contractUrl: String?,

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
     * 계약 금액(원). 의뢰인(갑)이 전문가(을)에게 지급하기로 한 용역 대금이다.
     */
    @field:NotNull(message = "계약 금액은 필수입니다.")
    @field:Min(value = 1, message = "계약 금액은 1원 이상이어야 합니다.")
    val price: Long?
)
