package spring.springserver.domain.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
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
