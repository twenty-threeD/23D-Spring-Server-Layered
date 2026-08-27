package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.global.data.BaseResponse

/**
 * 용역 구인/구직 커뮤니티.
 * 게시글 자체는 기존 커뮤니티와 같은 테이블을 쓰므로
 * 상세 조회·댓글·좋아요는 /api/community/post 쪽을 그대로 사용한다.
 */
@RestController
@RequestMapping("/api/community/job")
class CommunityJobPostController(
    private val communityJobPostService: CommunityJobPostService
) {

    @PostMapping
    fun createJobPost(
        @Valid @RequestBody createJobPostRequest: CreateJobPostRequest
    ): BaseResponse<CommunityPostResponse> {

        return BaseResponse.ok(communityJobPostService.createJobPost(createJobPostRequest))
    }

    @PatchMapping
    fun updateJobPost(
        @Valid @RequestBody updateJobPostRequest: UpdateJobPostRequest
    ): BaseResponse<CommunityPostResponse> {

        return BaseResponse.ok(communityJobPostService.updateJobPost(updateJobPostRequest))
    }

    @DeleteMapping
    fun deleteJobPost(
        @RequestParam postId: Long
    ): BaseResponse<DeleteResponse> {

        return BaseResponse.ok(communityJobPostService.deleteJobPost(postId))
    }

    @GetMapping
    fun getJobPosts(
        @ModelAttribute @Valid searchJobPostRequest: SearchJobPostRequest
    ): BaseResponse<List<CommunityPostResponse>> {

        return BaseResponse.ok(communityJobPostService.getJobPosts(searchJobPostRequest))
    }
}
