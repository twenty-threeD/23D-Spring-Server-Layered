package spring.springserver.domain.community.job.data.request

import jakarta.validation.constraints.NotNull

data class JobPostLikeRequest(
    @field:NotNull(message = "게시글 아이디는 필수입니다.")
    val postId: Long?,
)
