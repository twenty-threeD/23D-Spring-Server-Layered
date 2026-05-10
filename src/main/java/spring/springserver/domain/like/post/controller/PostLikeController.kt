package spring.springserver.domain.like.post.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.like.post.data.request.PostLikeRequest
import spring.springserver.domain.like.post.data.request.PostUnlikeRequest
import spring.springserver.domain.like.post.data.response.PostLikeResponse
import spring.springserver.domain.like.post.data.response.PostUnlikeResponse
import spring.springserver.domain.like.post.service.usecase.PostLikeUseCase
import spring.springserver.domain.like.post.service.usecase.PostUnLikeUseCase
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post")
class PostLikeController(private val likeUseCase: PostLikeUseCase,
                         private val unLikeUseCase: PostUnLikeUseCase) {

    @PostMapping("/like")
    fun like(postLikeRequest: PostLikeRequest): BaseResponse<PostLikeResponse> {

        return BaseResponse.ok(likeUseCase.like(postLikeRequest))
    }

    @PostMapping("/unlike")
    fun unlike(postUnlikeRequest: PostUnlikeRequest): BaseResponse<PostUnlikeResponse> {

        return BaseResponse.ok(unLikeUseCase.unlike(postUnlikeRequest))
    }
}