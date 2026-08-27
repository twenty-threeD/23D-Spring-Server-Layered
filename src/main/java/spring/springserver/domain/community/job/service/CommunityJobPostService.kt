package spring.springserver.domain.community.job.service

import spring.springserver.domain.community.job.data.request.CreateJobPostRequest
import spring.springserver.domain.community.job.data.request.UpdateJobPostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.entity.CommunityPostType

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
    ): CommunityPostResponse

    fun updateJobPost(
        updateJobPostRequest: UpdateJobPostRequest
    ): CommunityPostResponse

    /**
     * 구인/구직 목록. 넘기지 않은 조건은 필터에서 빠진다.
     *
     * @param nearbyOnly true면 기준 지역에서 NEARBY_RADIUS_KM 이내인 시군구만 본다.
     *                   기준 지역은 sigCd이고, 없으면 내 프로필에 설정된 지역이다.
     */
    fun getJobPosts(
        postType: CommunityPostType?,
        jobCategoryId: Long?,
        sigCd: String?,
        nearbyOnly: Boolean,
        keyword: String?
    ): List<CommunityPostResponse>
}
