package spring.springserver.domain.community.job.data.response

import org.springframework.data.domain.Page

/**
 * 구인/구직 목록 응답.
 *
 * 공용 PageResponse와 필드를 맞추되 nearbyFilterApplied를 더한다.
 * 중첩 없이 평평하게 두어 프론트가 content를 그대로 읽을 수 있게 한다.
 */
data class JobPostPageResponse(
    val content: List<CommunityJobPostResponse>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,

    val last: Boolean,

    /**
     * nearbyOnly를 요청했을 때 실제로 지역 필터가 걸렸는지.
     *
     * 기준 지역을 정할 수 없으면(비로그인이거나 프로필에 지역 미설정) 전국을 내려주는데,
     * 그 사실을 알리지 않으면 사용자는 전국 글을 "내 주변"으로 오해한다.
     * nearbyOnly를 요청하지 않았으면 null이다.
     */
    val nearbyFilterApplied: Boolean?,
) {

    companion object {

        fun of(
            page: Page<*>,
            content: List<CommunityJobPostResponse>,
            nearbyFilterApplied: Boolean?
        ): JobPostPageResponse {

            return JobPostPageResponse(
                content,
                page.number,
                page.size,
                page.totalElements,
                page.totalPages,
                page.isLast,
                nearbyFilterApplied,
            )
        }
    }
}
