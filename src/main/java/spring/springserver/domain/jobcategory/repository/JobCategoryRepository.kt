package spring.springserver.domain.jobcategory.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.jobcategory.entity.JobCategory

@Repository
interface JobCategoryRepository: JpaRepository<JobCategory, Long> {

    fun findAllByOrderByNameAsc(): List<JobCategory>

    fun findAllByParentIsNull(): List<JobCategory>

    @Query(
        """
        select c.id
        from JobCategory c
        where c.parent.id in :parentIds
        """
    )
    fun findIdsByParentIdIn(
        @Param("parentIds") parentIds: Collection<Long>
    ): List<Long>
}
