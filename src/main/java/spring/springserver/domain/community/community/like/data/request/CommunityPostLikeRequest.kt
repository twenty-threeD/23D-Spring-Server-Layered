package spring.springserver.domain.community.community.like.data.request

import jakarta.validation.constraints.NotNull

data class CommunityPostLikeRequest(
    @field:NotNull
    val postId: Long,
)