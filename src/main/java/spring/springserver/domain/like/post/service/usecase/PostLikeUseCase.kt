package spring.springserver.domain.like.post.service.usecase

import spring.springserver.domain.like.post.data.request.PostLikeRequest
import spring.springserver.domain.like.post.data.response.PostLikeResponse

interface PostLikeUseCase {

    fun like(postLikeRequest: PostLikeRequest): PostLikeResponse
}