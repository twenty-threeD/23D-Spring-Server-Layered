package spring.springserver.domain.community.like.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.community.like.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.domain.community.like.service.CommunityLikeService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community/like")
class CommunityLikeController(
    private val communityLikeService: CommunityLikeService
) {

    @PostMapping
    fun likeComment(@Valid @RequestBody communityCommentLikeRequest: CommunityCommentLikeRequest): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityLikeService.likeComment(communityCommentLikeRequest))
    }

    @DeleteMapping
    fun unlikeComment(@RequestParam commentId: Long): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityLikeService.unlikeComment(commentId))
    }
}