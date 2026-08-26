package spring.springserver.domain.contract.data.request

import jakarta.validation.constraints.NotNull

data class SignContractRequest(
    @field:NotNull(message = "계약서 아이디는 필수입니다.")
    val contractId: Long?
)
