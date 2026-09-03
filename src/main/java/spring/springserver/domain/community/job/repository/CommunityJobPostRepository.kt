package spring.springserver.domain.community.job.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.entity.JobPostType
import java.time.LocalDateTime

interface CommunityJobPostRepository : JpaRepository<CommunityJobPost, Long> {

    /**
     * 구인/구직 목록 조회.
     * 필터를 걸지 않은 축은 기준값을 null로 넘겨 통째로 건너뛴다.
     * 이때 in 절에 빈 컬렉션이 들어가지 않도록 서비스에서 더미 값을 채워 넘긴다.
     */
    @Query(
        """
        select p
        from CommunityJobPost p
        join fetch p.jobCategory jobCategory
        join fetch p.sig sig
        where p.deletedAt is null
          and p.postType in :postTypes
          and (:jobCategoryId is null or jobCategory.id in :jobCategoryIds)
          and (:sigCdFilter is null or sig.sigCd in :sigCds)
          and (
              :keyword = ''
              or coalesce(lower(p.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(p.username), '') like lower(concat('%', :keyword, '%'))
          )
        order by p.updatedAt desc
        """
    )
    fun searchJobPosts(
        @Param("postTypes") postTypes: Collection<JobPostType>,
        @Param("jobCategoryId") jobCategoryId: Long?,
        @Param("jobCategoryIds") jobCategoryIds: Collection<Long>,
        @Param("sigCdFilter") sigCdFilter: String?,
        @Param("sigCds") sigCds: Collection<String>,
        @Param("keyword") keyword: String
    ): List<CommunityJobPost>

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityJobPost?

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityJobPost>
}
