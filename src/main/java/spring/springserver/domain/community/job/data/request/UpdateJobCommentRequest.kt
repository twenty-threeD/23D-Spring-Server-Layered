package spring.springserver.domain.community.job.data.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateJobCommentRequest(
    @field:NotNull(message = "댓글 아이디는 필수입니다.")
    val commentId: Long?,

    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
    val content: String?,
)
