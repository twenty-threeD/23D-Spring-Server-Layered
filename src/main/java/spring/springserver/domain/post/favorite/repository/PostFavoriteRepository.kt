package spring.springserver.domain.post.favorite.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.favorite.entity.PostFavorite
import java.time.LocalDateTime

interface PostFavoriteRepository: JpaRepository<PostFavorite, Long> {

    fun existsByMemberAndPost(
        member: Member,
        post: Post
    ): Boolean

    fun countByPostId(
        postId: Long
    ): Long

    fun deleteByMemberAndPost(
        member: Member,
        post: Post
    ): Long

    /**
     * 보관 기간이 지난 소프트 삭제 게시글의 즐겨찾기를 정리한다.
     * 첨부 파일 정리와 조건이 다르므로 게시글을 로드하지 않고 즐겨찾기 기준으로 바로 지운다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        delete from PostFavorite f
        where f.post in (
            select p
            from Post p
            where p.isDeleted = true
              and p.deletedAt < :deletedAt
        )
        """
    )
    fun deleteAllByExpiredPosts(
        @Param("deletedAt") deletedAt: LocalDateTime
    ): Int

    @Query(
        """
        select pf.post
        from PostFavorite pf
        where pf.member = :member
          and pf.post.isDeleted = false
        order by pf.post.updatedAt desc
        """
    )
    fun findFavoritePostsByMember(
        @Param("member") member: Member,
        pageable: Pageable
    ): Page<Post>
}
