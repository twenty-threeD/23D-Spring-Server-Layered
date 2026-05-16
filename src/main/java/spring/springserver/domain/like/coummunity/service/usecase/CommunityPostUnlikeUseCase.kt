package spring.springserver.domain.like.coummunity.service.usecase

import spring.springserver.domain.like.coummunity.data.request.CommunityPostUnlikeRequest
import spring.springserver.domain.like.coummunity.data.response.CommunityPostUnlikeResponse

interface CommunityPostUnlikeUseCase {

    fun unlike(communityPostUnlikeRequest: CommunityPostUnlikeRequest): CommunityPostUnlikeResponse
}