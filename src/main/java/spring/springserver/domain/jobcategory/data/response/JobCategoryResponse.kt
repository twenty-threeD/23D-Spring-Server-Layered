package spring.springserver.domain.jobcategory.data.response

import spring.springserver.domain.jobcategory.entity.JobCategory

data class JobCategoryResponse(
    val id: Long?,

    val name: String,

    val parentId: Long?,

    val parentName: String?,

    val fullName: String
) {

    companion object {

        fun of(jobCategory: JobCategory): JobCategoryResponse {

            return JobCategoryResponse(
                jobCategory.getId(),
                jobCategory.name,
                jobCategory.parent?.getId(),
                jobCategory.parent?.name,
                jobCategory.getFullName()
            )
        }
    }
}