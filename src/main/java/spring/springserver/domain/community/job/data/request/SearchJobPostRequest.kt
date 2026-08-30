package spring.springserver.domain.community.job.data.request

import jakarta.validation.constraints.Size
import spring.springserver.domain.community.job.entity.JobPostType

/**
 * 구인/구직 목록 조회 조건. 넘기지 않은 값은 그 축의 필터를 걸지 않는다.
 * 조건이 늘어나면 여기에 필드를 추가한다.
 */
data class SearchJobPostRequest(
    /**
     * HIRING 또는 SEEKING. 비우면 구인·구직을 모두 본다.
     */
    val postType: JobPostType? = null,

    /**
     * 지정한 카테고리와 그 하위 카테고리 글까지 함께 조회된다.
     */
    val jobCategoryId: Long? = null,

    @field:Size(min = 5, max = 5, message = "시군구 코드는 5자리입니다.")
    val sigCd: String? = null,

    /**
     * true면 기준 지역에서 반경 안에 드는 시군구만 본다.
     * 기준 지역은 sigCd이고, 비어 있으면 내 프로필에 설정된 지역이다.
     */
    val nearbyOnly: Boolean = false,

    val keyword: String? = null
)
