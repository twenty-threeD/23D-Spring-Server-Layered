package spring.springserver.domain.community.data.request

import jakarta.validation.constraints.NotNull

data class CommunityCommentLikeRequest(
    @field:NotNull
    val commentId: Long,
)
