package spring.springserver.domain.like.post.service.usecase

import spring.springserver.domain.like.post.data.request.PostUnlikeRequest
import spring.springserver.domain.like.post.data.response.PostUnlikeResponse

interface PostUnLikeUseCase {

    fun unlike(postUnlikeRequest: PostUnlikeRequest): PostUnlikeResponse
}