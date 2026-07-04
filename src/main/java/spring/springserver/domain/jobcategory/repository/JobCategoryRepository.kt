package spring.springserver.domain.jobcategory.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import spring.springserver.domain.jobcategory.entity.JobCategory

interface JobCategoryRepository: JpaRepository<JobCategory, Long> {

    @Query(
        """
        select j
        from JobCategory j
        where not exists (
            select 1
            from JobCategory child
            where child.parent = j
        )
        order by j.name asc
        """
    )
    fun findAllLeafCategories(): List<JobCategory>
}