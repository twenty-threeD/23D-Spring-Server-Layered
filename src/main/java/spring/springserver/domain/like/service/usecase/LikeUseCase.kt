package spring.springserver.domain.like.service.usecase

import spring.springserver.domain.like.data.response.LikeResponse

interface LikeUseCase {

    fun like(memberId: Long): LikeResponse
}