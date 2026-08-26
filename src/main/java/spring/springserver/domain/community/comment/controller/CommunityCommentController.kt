package spring.springserver.domain.community.comment.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.community.comment.data.request.CreateCommentRequest
import spring.springserver.domain.community.comment.data.request.UpdateCommentRequest
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.service.CommunityCommentService
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community/comment")
class CommunityCommentController(
    private val communityCommentService: CommunityCommentService
) {

    @PostMapping
    fun createComment(
        @Valid @RequestBody createCommentRequest: CreateCommentRequest
    ): BaseResponse<CommunityCommentResponse> {

        return BaseResponse.ok(communityCommentService.createComment(createCommentRequest))
    }

    @GetMapping
    fun getComments(
        @RequestParam postId: Long
    ): BaseResponse<List<CommunityCommentResponse>> {

        return BaseResponse.ok(communityCommentService.getComments(postId))
    }

    @PatchMapping
    fun updateComment(
        @Valid @RequestBody updateCommentRequest: UpdateCommentRequest
    ): BaseResponse<CommunityCommentResponse> {

        return BaseResponse.ok(communityCommentService.updateComment(updateCommentRequest))
    }

    @DeleteMapping
    fun deleteComment(
        @RequestParam commentId: Long
    ): BaseResponse<DeleteResponse> {

        return BaseResponse.ok(communityCommentService.deleteComment(commentId))
    }
}