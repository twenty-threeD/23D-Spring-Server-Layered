package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.SearchJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.job.data.response.CommunityJobPostResponse

interface CommunityJobPostService {

    companion object {

        /**
         * 내 주변으로 볼 반경(km). 알림 발송 반경과 같은 값을 쓴다.
         */
        const val NEARBY_RADIUS_KM = 5.0
    }

    /**
     * 전화번호가 인증된 회원만 구인/구직 글을 쓸 수 있다.
     * 저장이 끝나면 조건에 맞는 회원에게 알림을 보내도록 이벤트를 발행한다.
     */
    fun createJobPost(
        createJobPostRequest: CreateJobPostRequest
    ): CommunityJobPostResponse

    fun updateJobPost(
        updateJobPostRequest: UpdateJobPostRequest
    ): CommunityJobPostResponse

    /**
     * 작성자만 지울 수 있다. 소프트 삭제이므로 보관 기간이 지나면
     * CommunityRetentionService가 댓글·좋아요까지 함께 정리한다.
     */
    fun deleteJobPost(
        postId: Long
    ): DeleteResponse

    /**
     * 구인/구직 목록. 넘기지 않은 조건은 필터에서 빠진다.
     * nearbyOnly가 켜지면 기준 지역에서 NEARBY_RADIUS_KM 이내인 시군구만 본다.
     */
    fun getJobPosts(
        searchJobPostRequest: SearchJobPostRequest
    ): List<CommunityJobPostResponse>

    /**
     * 구인/구직 상세. 게시글 테이블이 갈렸으므로 일반 커뮤니티 상세로는 열리지 않는다.
     */
    fun getJobPost(
        postId: Long
    ): CommunityJobPostResponse
}
