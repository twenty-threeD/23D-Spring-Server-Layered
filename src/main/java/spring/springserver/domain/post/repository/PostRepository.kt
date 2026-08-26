package spring.springserver.domain.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

@Repository
interface PostRepository: JpaRepository<Post, Long> {

    fun findPostById(
        id: Long
    ): Post?

    fun findAllByIsDeletedFalseOrderByUpdatedAtDesc(
        pageable: Pageable
    ): Page<Post>

    fun findAllByMemberAndIsDeletedFalseOrderByUpdatedAtDesc(
        member: Member
    ): List<Post>

    fun findAllByIsDeletedTrueAndDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<Post>

    @Query(
        """
        select p
        from Post p
        where p.isDeleted = false
          and (
              :title = ''
              or lower(p.title) like lower(concat('%', :title, '%'))
          )
        order by p.updatedAt desc
        """
    )
    fun searchPostsByTitle(
        @Param("title") title: String,
        pageable: Pageable
    ): Page<Post>

    /**
     * categoryIds에는 검색 대상 카테고리와 그 하위 카테고리 id가 모두 담겨 오므로,
     * 상위 카테고리로 검색해도 하위 카테고리 게시글이 함께 조회된다.
     */
    @Query(
        """
        select p
        from Post p
        where p.isDeleted = false
          and (
              :title = ''
              or lower(p.title) like lower(concat('%', :title, '%'))
          )
          and p.category.id in :categoryIds
        order by p.updatedAt desc
        """
    )
    fun searchPostsByTitleAndCategoryIds(
        @Param("title") title: String,
        @Param("categoryIds") categoryIds: Collection<Long>,
        pageable: Pageable
    ): Page<Post>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
update Post p
set p.viewCount = p.viewCount + 1
where p.id = :id and p.isDeleted = false
"""
    )
    fun incrementViewCount(
        @Param("id") id: Long
    ): Int
}
