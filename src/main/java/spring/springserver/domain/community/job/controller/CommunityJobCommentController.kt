package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobCommentRequest
import spring.springserver.domain.community.job.data.request.UpdateJobCommentRequest
import spring.springserver.domain.community.job.data.response.CommunityJobCommentResponse
import spring.springserver.domain.community.job.service.CommunityJobCommentService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.data.PageResponse

@RestController
@RequestMapping("/api/jobs/comment")
class CommunityJobCommentController(
    private val communityJobCommentService: CommunityJobCommentService
) {

    @PostMapping
    fun createJobComment(
        @Valid @RequestBody createJobCommentRequest: CreateJobCommentRequest
    ): BaseResponse<CommunityJobCommentResponse> {

        return BaseResponse.ok(communityJobCommentService.createJobComment(createJobCommentRequest))
    }

    @GetMapping
    fun getJobComments(
        @RequestParam postId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): BaseResponse<PageResponse<CommunityJobCommentResponse>> {

        return BaseResponse.ok(communityJobCommentService.getJobComments(postId, page, size))
    }

    @PatchMapping
    fun updateJobComment(
        @Valid @RequestBody updateJobCommentRequest: UpdateJobCommentRequest
    ): BaseResponse<CommunityJobCommentResponse> {

        return BaseResponse.ok(communityJobCommentService.updateJobComment(updateJobCommentRequest))
    }

    @DeleteMapping
    fun deleteJobComment(
        @RequestParam commentId: Long
    ): BaseResponse<DeleteResponse> {

        return BaseResponse.ok(communityJobCommentService.deleteJobComment(commentId))
    }
}
