package spring.springserver.domain.community.job.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.entity.JobPostType
import java.time.LocalDateTime

interface CommunityJobPostRepository : JpaRepository<CommunityJobPost, Long> {

    /**
     * 구인/구직 목록 조회.
     * 필터를 걸지 않은 축은 applyXxxFilter를 false로 넘겨 통째로 건너뛴다.
     * 이때 in 절에 빈 컬렉션이 들어가지 않도록 서비스에서 더미 값을 채워 넘긴다.
     *
     * 필터 on/off를 별도 boolean으로 받는 이유는, 목록이 비어 있다는 사실과
     * 필터를 걸지 않는다는 사실을 한 값으로 겸하면 "주변에 아무것도 없음"이
     * "전국 조회"로 뒤집히기 때문이다.
     */
    @Query(
        value = """
        select p.id
        from CommunityJobPost p
        where p.deletedAt is null
          and p.postType in :postTypes
          and (:applyCategoryFilter = false or p.jobCategory.id in :jobCategoryIds)
          and (:applySigFilter = false or p.sig.sigCd in :sigCds)
          and (
              :keyword = ''
              or coalesce(lower(p.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(p.content), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(p.username), '') like lower(concat('%', :keyword, '%'))
          )
        order by p.updatedAt desc, p.id desc
        """,
        countQuery = """
        select count(p.id)
        from CommunityJobPost p
        where p.deletedAt is null
          and p.postType in :postTypes
          and (:applyCategoryFilter = false or p.jobCategory.id in :jobCategoryIds)
          and (:applySigFilter = false or p.sig.sigCd in :sigCds)
          and (
              :keyword = ''
              or coalesce(lower(p.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(p.content), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(p.username), '') like lower(concat('%', :keyword, '%'))
          )
        """
    )
    fun searchJobPostIds(
        @Param("postTypes") postTypes: Collection<JobPostType>,
        @Param("applyCategoryFilter") applyCategoryFilter: Boolean,
        @Param("jobCategoryIds") jobCategoryIds: Collection<Long>,
        @Param("applySigFilter") applySigFilter: Boolean,
        @Param("sigCds") sigCds: Collection<String>,
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Long>

    /**
     * 페이징된 id로 본문과 연관을 한 번에 가져온다.
     *
     * 페이징과 join fetch를 한 쿼리에 같이 쓰면 Hibernate가 전체를 읽어 메모리에서 자르므로
     * id만 페이징한 뒤 이 쿼리로 채우는 두 단계로 나눈다.
     */
    @Query(
        """
        select p
        from CommunityJobPost p
        join fetch p.jobCategory
        join fetch p.sig
        join fetch p.member
        where p.id in :postIds
        """
    )
    fun findAllWithAssociationsByIds(
        @Param("postIds") postIds: Collection<Long>
    ): List<CommunityJobPost>

    /**
     * 목록용 좋아요 수 집계. 글마다 count 쿼리를 날리지 않기 위해 한 번에 가져온다.
     */
    @Query(
        """
        select l.communityJobPost.id as postId, count(l.id) as count
        from CommunityJobPostLike l
        where l.communityJobPost.id in :postIds
        group by l.communityJobPost.id
        """
    )
    fun countLikesByPostIds(
        @Param("postIds") postIds: Collection<Long>
    ): List<PostCountProjection>

    /**
     * 현재 회원이 좋아요를 누른 글의 id 목록. 목록 응답의 isLiked를 한 번에 채운다.
     */
    @Query(
        """
        select l.communityJobPost.id
        from CommunityJobPostLike l
        where l.communityJobPost.id in :postIds
          and l.member.id = :memberId
        """
    )
    fun findLikedPostIds(
        @Param("postIds") postIds: Collection<Long>,
        @Param("memberId") memberId: Long
    ): List<Long>

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityJobPost?

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityJobPost>
}
