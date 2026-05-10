package spring.springserver.domain.like.coummunity.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.like.coummunity.data.request.CommunityPostLikeRequest
import spring.springserver.domain.like.coummunity.data.request.CommunityPostUnlikeRequest
import spring.springserver.domain.like.coummunity.data.response.CommunityPostLikeResponse
import spring.springserver.domain.like.coummunity.data.response.CommunityPostUnlikeResponse
import spring.springserver.domain.like.coummunity.service.usecase.CommunityPostLikeUseCase
import spring.springserver.domain.like.coummunity.service.usecase.CommunityPostUnlikeUseCase
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community")
class CommunityPostLikeController(private val communityPostLikeUseCase: CommunityPostLikeUseCase,
                                  private val communityPostUnlikeUseCase: CommunityPostUnlikeUseCase) {

    @PostMapping("/like")
    fun like(communityPostLikeRequest: CommunityPostLikeRequest): BaseResponse<CommunityPostLikeResponse> {

        return BaseResponse.ok(communityPostLikeUseCase.like(communityPostLikeRequest))
    }

    @PostMapping("/unlike")
    fun unlike(communityPostUnlikeRequest: CommunityPostUnlikeRequest): BaseResponse<CommunityPostUnlikeResponse> {

        return BaseResponse.ok(communityPostUnlikeUseCase.unlike(communityPostUnlikeRequest))
    }
}