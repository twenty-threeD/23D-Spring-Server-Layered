package spring.springserver.domain.community.job.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.service.CommunityJobPostService
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.entity.CommunityPostType
import spring.springserver.global.data.BaseResponse

/**
 * 용역 구인/구직 커뮤니티.
 * 게시글 자체는 기존 커뮤니티와 같은 테이블을 쓰므로
 * 상세 조회·삭제·댓글·좋아요는 /api/community/post 쪽을 그대로 사용한다.
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

    @GetMapping
    fun getJobPosts(
        @RequestParam(required = false) postType: CommunityPostType?,
        @RequestParam(required = false) jobCategoryId: Long?,
        @RequestParam(required = false) sigCd: String?,
        @RequestParam(required = false, defaultValue = "false") nearbyOnly: Boolean,
        @RequestParam(required = false) keyword: String?
    ): BaseResponse<List<CommunityPostResponse>> {

        return BaseResponse.ok(communityJobPostService.getJobPosts(
            postType,
            jobCategoryId,
            sigCd,
            nearbyOnly,
            keyword
        ))
    }
}
