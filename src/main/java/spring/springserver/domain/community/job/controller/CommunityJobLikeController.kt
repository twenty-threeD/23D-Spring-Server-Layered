package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.community.job.data.request.JobPostLikeRequest
import spring.springserver.domain.community.job.service.CommunityJobLikeService
import spring.springserver.domain.community.like.data.response.CommunityLikeResponse
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/community/job/like")
class CommunityJobLikeController(
    private val communityJobLikeService: CommunityJobLikeService
) {

    @PostMapping
    fun likeJobPost(
        @Valid @RequestBody jobPostLikeRequest: JobPostLikeRequest
    ): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityJobLikeService.likeJobPost(jobPostLikeRequest))
    }

    @DeleteMapping
    fun unlikeJobPost(
        @RequestParam postId: Long
    ): BaseResponse<CommunityLikeResponse> {

        return BaseResponse.ok(communityJobLikeService.unlikeJobPost(postId))
    }
}
