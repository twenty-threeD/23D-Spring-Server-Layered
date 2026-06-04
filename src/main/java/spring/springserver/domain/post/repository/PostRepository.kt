package spring.springserver.domain.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

@Repository
interface PostRepository: JpaRepository<Post, Long> {

    fun findPostById(id: Long): Post?

    fun findAllByIsDeletedTrueAndDeletedAtBefore(deletedAt: LocalDateTime): List<Post>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p " +
            "set p.viewCount = p.viewCount + 1 " +
            "where p.id = :id and p.isDeleted = false")
    fun incrementViewCount(@Param("id") id: Long): Int
}
