package spring.springserver.domain.profile.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import spring.springserver.domain.profile.entity.MovableDistance

data class UpdateProfileRequest(
    @field:NotBlank
    @field:Pattern(
        regexp = "^[가-힣a-zA-Z0-9]{2,30}$",
        message = "닉네임은 2자 이상 30자 이하의 한글, 영문, 숫자만 사용할 수 있습니다."
    )
    val nickname: String,

    val imageUrl: String?,

    val sigCd: String?,

    val movableDistance: MovableDistance?,

    @field:Size(max = 100, message = "짧은 설명은 100자 이하로 입력해주세요.")
    val shortDescription: String?,

    val jobCategoryId: Long?
)