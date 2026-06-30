package spring.springserver.domain.jobcategory.service

import spring.springserver.domain.jobcategory.data.response.JobCategoryResponse
import spring.springserver.domain.jobcategory.entity.JobCategory

interface JobCategoryService {

    fun getJobCategories(): List<JobCategoryResponse>

    fun getJobCategory(
        jobCategoryId: Long
    ): JobCategory
}