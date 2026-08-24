package spring.springserver.domain.estimate.data.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateEstimateRequest(
    @field:NotNull(message = "게시글 아이디는 필수입니다.")
    val postId: Long?,

    @field:NotNull(message = "의뢰인 아이디는 필수입니다.")
    val clientId: Long?,

    @field:NotBlank(message = "견적서 URL은 필수입니다.")
    @field:Size(max = 2048, message = "견적서 URL은 2048자 이하로 입력해주세요.")
    val url: String?,

    @field:NotNull(message = "최종 금액은 필수입니다.")
    @field:Min(value = 0, message = "최종 금액은 0원 이상이어야 합니다.")
    val totalPay: Long?
)
