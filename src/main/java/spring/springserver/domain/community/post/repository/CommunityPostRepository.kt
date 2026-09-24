package spring.springserver.domain.community.post.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import java.time.LocalDateTime

interface CommunityPostRepository : JpaRepository<CommunityPost, Long> {

    @EntityGraph(attributePaths = ["member"])
    @Query(
        value = """
        select c
        from CommunityPost c
        left join c.member m
        where c.deletedAt is null
          and (
              :keyword = ''
              or coalesce(lower(c.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(c.username), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(m.username), '') like lower(concat('%', :keyword, '%'))
          )
        order by c.updatedAt desc
        """,
        countQuery = """
        select count(c)
        from CommunityPost c
        left join c.member m
        where c.deletedAt is null
          and (
              :keyword = ''
              or coalesce(lower(c.title), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(c.username), '') like lower(concat('%', :keyword, '%'))
              or coalesce(lower(m.username), '') like lower(concat('%', :keyword, '%'))
          )
        """
    )
    fun searchPosts(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<CommunityPost>

    @EntityGraph(attributePaths = ["member"])
    @Query(
        """
        select c
        from CommunityPost c
        where c.category = :category
        and c.deletedAt is null
        order by c.updatedAt desc
        """
    )
    fun searchPostsByCategory(
        @Param("category") category: Category,
        pageable: Pageable
    ): Page<CommunityPost>

    @EntityGraph(attributePaths = ["member"])
    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityPost?

    @EntityGraph(attributePaths = ["member"])
    fun findAllByDeletedAtIsNullOrderByUpdatedAtDesc(
        pageable: Pageable
    ): Page<CommunityPost>

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityPost>
}
