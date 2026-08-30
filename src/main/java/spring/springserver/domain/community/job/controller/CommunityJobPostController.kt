package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.data.response.CommunityJobPostResponse
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.global.data.BaseResponse

/**
 * 용역 구인/구직 커뮤니티.
 * 일반 커뮤니티와 게시글 테이블이 다르므로 상세 조회·댓글·좋아요도 이 아래에 둔다.
 */
@RestController
@RequestMapping("/api/community/job")
class CommunityJobPostController(
    private val communityJobPostService: CommunityJobPostService
) {

    @PostMapping
    fun createJobPost(
        @Valid @RequestBody createJobPostRequest: CreateJobPostRequest
    ): BaseResponse<CommunityJobPostResponse> {

        return BaseResponse.ok(communityJobPostService.createJobPost(createJobPostRequest))
    }

    @PatchMapping
    fun updateJobPost(
        @Valid @RequestBody updateJobPostRequest: UpdateJobPostRequest
    ): BaseResponse<CommunityJobPostResponse> {

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
    ): BaseResponse<List<CommunityJobPostResponse>> {

        return BaseResponse.ok(communityJobPostService.getJobPosts(searchJobPostRequest))
    }

    /**
     * comment·like 컨트롤러가 쓰는 /api/community/job/comment 같은 고정 경로는
     * 경로 변수보다 먼저 매칭되므로 이 매핑과 부딪히지 않는다.
     */
    @GetMapping("/{postId}")
    fun getJobPost(
        @PathVariable postId: Long
    ): BaseResponse<CommunityJobPostResponse> {

        return BaseResponse.ok(communityJobPostService.getJobPost(postId))
    }
}
