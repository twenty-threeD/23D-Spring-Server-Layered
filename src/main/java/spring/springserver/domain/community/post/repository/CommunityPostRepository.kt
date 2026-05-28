package spring.springserver.domain.community.post.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.post.entity.CommunityPost
import java.time.LocalDateTime

interface CommunityPostRepository : JpaRepository<CommunityPost, Long> {

    @Query(
        """
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
        order by c.createdAt desc
        """
    )
    fun searchPosts(@Param("keyword") keyword: String): List<CommunityPost>

    fun findByIdAndDeletedAtIsNull(id: Long): CommunityPost?

    fun findAllByDeletedAtBefore(deletedAt: LocalDateTime): List<CommunityPost>
}
