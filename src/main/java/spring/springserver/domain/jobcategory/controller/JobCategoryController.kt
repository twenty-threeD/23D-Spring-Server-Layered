package spring.springserver.domain.jobcategory.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.jobcategory.data.response.JobCategoryResponse
import spring.springserver.domain.jobcategory.service.JobCategoryService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/job-category")
class JobCategoryController(
    private val jobCategoryService: JobCategoryService
) {

    @GetMapping
    fun getJobCategories(): BaseResponse<List<JobCategoryResponse>> {

        return BaseResponse.ok(jobCategoryService.getJobCategories())
    }
}