package spring.springserver.domain.community.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.entity.CommunityPostType
import java.time.LocalDateTime

interface CommunityPostRepository : JpaRepository<CommunityPost, Long> {

    @Query(
        """
        select c
        from CommunityPost c
        left join c.member m
        where c.deletedAt is null
          and c.postType = :postType
          and (
              :keyword = ''
              or coalesce(lower(c.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(c.username), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(m.username), '') like lower(concat('%', :keyword, '%'))
          )
        order by c.updatedAt desc
        """
    )
    fun searchPosts(
        @Param("keyword") keyword: String,
        @Param("postType") postType: CommunityPostType
    ): List<CommunityPost>

    /**
     * 구인/구직 목록 조회.
     * 필터를 걸지 않은 축은 기준값을 null로 넘겨 통째로 건너뛴다.
     * 이때 in 절에 빈 컬렉션이 들어가지 않도록 서비스에서 더미 값을 채워 넘긴다.
     */
    @Query(
        """
        select p
        from CommunityPost p
        left join fetch p.jobCategory jobCategory
        left join fetch p.sig sig
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
        @Param("postTypes") postTypes: Collection<CommunityPostType>,
        @Param("jobCategoryId") jobCategoryId: Long?,
        @Param("jobCategoryIds") jobCategoryIds: Collection<Long>,
        @Param("sigCdFilter") sigCdFilter: String?,
        @Param("sigCds") sigCds: Collection<String>,
        @Param("keyword") keyword: String
    ): List<CommunityPost>

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityPost?

    fun findAllByPostTypeAndDeletedAtIsNullOrderByUpdatedAtDesc(
        postType: CommunityPostType
    ): List<CommunityPost>

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityPost>
}
