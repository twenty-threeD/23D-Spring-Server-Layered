package spring.springserver.domain.community.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.community.data.request.CommunityCommentLikeRequest
import spring.springserver.domain.community.data.request.CreateCommentRequest
import spring.springserver.domain.community.data.request.UpdateCommentRequest
import spring.springserver.domain.community.data.response.CommunityCommentResponse
import spring.springserver.domain.community.data.response.CommunityLikeResponse
import spring.springserver.domain.community.data.response.DeleteResponse
import spring.springserver.domain.community.service.comment.CommunityCommentService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community/comment")
class CommunityCommentController(private val communityCommentService: CommunityCommentService) {

    @PostMapping
    fun createComment(@RequestBody @Valid createCommentRequest: CreateCommentRequest): BaseResponse<CommunityCommentResponse> {

        return BaseResponse.ok(communityCommentService.createComment(createCommentRequest))
    }

    @GetMapping
    fun getComments(@RequestParam postId: Long): BaseResponse<List<CommunityCommentResponse>> {

        return BaseResponse.ok(communityCommentService.getComments(postId))
    }

    @PatchMapping
    fun updateComment(@RequestBody @Valid updateCommentRequest: UpdateCommentRequest): BaseResponse<CommunityCommentResponse> {

        return BaseResponse.ok(communityCommentService.updateComment(updateCommentRequest))
    }

    @DeleteMapping
    fun deleteComment(@RequestParam commentId: Long): BaseResponse<DeleteResponse> {

        communityCommentService.deleteComment(commentId)

        return BaseResponse.ok(DeleteResponse.ok())
    }

    @PostMapping("/like")
    fun likeComment(@RequestBody @Valid communityCommentLikeRequest: CommunityCommentLikeRequest): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityCommentService.likeComment(communityCommentLikeRequest))
    }

    @DeleteMapping("/like")
    fun unlikeComment(@RequestParam commentId: Long): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityCommentService.unlikeComment(commentId))
    }
}
