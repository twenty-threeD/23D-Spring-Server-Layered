package spring.springserver.domain.like.service.usecase

import spring.springserver.domain.like.data.response.UnlikeResponse

interface UnLikeUseCase {

    fun unlike(memberId: Long): UnlikeResponse
}