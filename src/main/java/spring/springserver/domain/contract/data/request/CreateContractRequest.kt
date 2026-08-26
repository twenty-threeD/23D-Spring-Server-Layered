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
     * 계약 당사자 갑의 회원 아이디.
     */
    @field:NotNull(message = "갑 회원 아이디는 필수입니다.")
    val partA: Long?,

    /**
     * 계약 당사자 을의 회원 아이디.
     */
    @field:NotNull(message = "을 회원 아이디는 필수입니다.")
    val partB: Long?,

    @field:NotNull(message = "계약 금액은 필수입니다.")
    @field:Min(value = 0, message = "계약 금액은 0원 이상이어야 합니다.")
    val amount: Long?
)
