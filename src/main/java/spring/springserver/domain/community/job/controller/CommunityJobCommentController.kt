package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobCommentRequest
import spring.springserver.domain.community.job.data.request.UpdateJobCommentRequest
import spring.springserver.domain.community.job.data.response.CommunityJobCommentResponse
import spring.springserver.domain.community.job.service.CommunityJobCommentService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community/job/comment")
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
        @RequestParam postId: Long
    ): BaseResponse<List<CommunityJobCommentResponse>> {

        return BaseResponse.ok(communityJobCommentService.getJobComments(postId))
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
