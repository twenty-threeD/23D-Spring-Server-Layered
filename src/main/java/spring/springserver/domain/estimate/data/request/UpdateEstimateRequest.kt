package spring.springserver.domain.estimate.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateEstimateRequest(
    @field:NotBlank(message = "견적서 URL은 필수입니다.")
    @field:Size(max = 500, message = "견적서 URL은 500자 이하로 입력해주세요.")
    val url: String?
)
