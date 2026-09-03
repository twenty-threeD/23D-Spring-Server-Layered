package spring.springserver.domain.post.review.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.review.entity.PostReview
import java.time.LocalDateTime

@Repository
interface PostReviewRepository: JpaRepository<PostReview, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): PostReview?

    fun findByMemberAndPost(
        member: Member,
        post: Post
    ): PostReview?

    fun countByPostIdAndDeletedAtIsNull(
        postId: Long
    ): Long

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<PostReview>

    fun deleteAllByPostIn(
        posts: Collection<Post>
    )

    @Query(
        value = """
        select pr
        from PostReview pr
        join fetch pr.member
        where pr.post.id = :postId
          and pr.deletedAt is null
        order by pr.createdAt desc
        """,
        countQuery = """
        select count(pr)
        from PostReview pr
        where pr.post.id = :postId
          and pr.deletedAt is null
        """
    )
    fun findActiveReviewsByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): Page<PostReview>

    @Query(
        """
        select avg(pr.rating)
        from PostReview pr
        where pr.post.id = :postId
          and pr.deletedAt is null
        """
    )
    fun findAverageRatingByPostId(
        @Param("postId") postId: Long
    ): Double?
}