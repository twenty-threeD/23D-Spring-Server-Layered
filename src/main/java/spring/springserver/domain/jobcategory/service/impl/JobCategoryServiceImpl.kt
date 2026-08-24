package spring.springserver.domain.jobcategory.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.jobcategory.data.response.JobCategoryResponse
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.jobcategory.exception.JobCategoryStatusCode
import spring.springserver.domain.jobcategory.repository.JobCategoryRepository
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(readOnly = true)
class JobCategoryServiceImpl(
    private val jobCategoryRepository: JobCategoryRepository
): JobCategoryService {

    override fun getJobCategories(): List<JobCategoryResponse> {

        return jobCategoryRepository.findAllByOrderByNameAsc()
            .map {
                jobCategory -> JobCategoryResponse.of(jobCategory)
            }
    }

    override fun getJobCategory(
        jobCategoryId: Long
    ): JobCategory {

        return jobCategoryRepository.findById(jobCategoryId).orElse(null)
            ?: throw ApplicationException(JobCategoryStatusCode.JOB_CATEGORY_NOT_FOUND)
    }

    override fun getCategoryIdsIncludingDescendants(
        jobCategoryId: Long
    ): List<Long> {

        val rootId = getJobCategory(jobCategoryId).getId()!!

        val collected = linkedSetOf(rootId)
        var currentLevel = listOf(rootId)

        while (currentLevel.isNotEmpty()) {

            // 이미 담긴 id는 걸러내므로, 데이터가 순환하더라도 무한 루프가 되지 않는다.
            currentLevel = jobCategoryRepository.findIdsByParentIdIn(currentLevel)
                .filter { childId -> collected.add(childId) }
        }

        return collected.toList()
    }
}
