package spring.springserver.domain.like.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.like.data.request.LikeRequest
import spring.springserver.domain.like.data.request.UnlikeRequest
import spring.springserver.domain.like.data.response.LikeResponse
import spring.springserver.domain.like.data.response.UnlikeResponse
import spring.springserver.domain.like.service.usecase.LikeUseCase
import spring.springserver.domain.like.service.usecase.UnLikeUseCase
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/likes")
class LikeController(private val likeUseCase: LikeUseCase,
                     private val unLikeUseCase: UnLikeUseCase) {

    @PostMapping("/like")
    fun like(likeRequest: LikeRequest): BaseResponse<LikeResponse> {

        return BaseResponse.ok(likeUseCase.like(likeRequest.memberId))
    }

    @PostMapping("/unlike")
    fun unlike(unlikeRequest: UnlikeRequest): BaseResponse<UnlikeResponse> {

        return BaseResponse.ok(unLikeUseCase.unlike(unlikeRequest.memberId))
    }
}