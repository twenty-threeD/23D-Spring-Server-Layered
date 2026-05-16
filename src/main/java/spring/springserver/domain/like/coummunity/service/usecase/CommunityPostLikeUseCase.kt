package spring.springserver.domain.like.coummunity.service.usecase

import spring.springserver.domain.like.coummunity.data.request.CommunityPostLikeRequest
import spring.springserver.domain.like.coummunity.data.response.CommunityPostLikeResponse

interface CommunityPostLikeUseCase{

    fun like(communityLikeRequest: CommunityPostLikeRequest): CommunityPostLikeResponse
}