package spring.springserver.domain.jobcategory.service

import spring.springserver.domain.jobcategory.data.response.JobCategoryResponse
import spring.springserver.domain.jobcategory.entity.JobCategory

interface JobCategoryService {

    fun getJobCategories(): List<JobCategoryResponse>

    fun getJobCategory(
        jobCategoryId: Long
    ): JobCategory

    /**
     * 주어진 카테고리와 그 하위 카테고리 전체의 id를 반환한다.
     * 상위 카테고리로 검색했을 때 하위 카테고리 게시글까지 조회하기 위해 쓰인다.
     */
    fun getCategoryIdsIncludingDescendants(
        jobCategoryId: Long
    ): List<Long>
}
