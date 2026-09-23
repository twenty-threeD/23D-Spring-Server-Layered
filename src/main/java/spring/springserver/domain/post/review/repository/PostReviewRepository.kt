package spring.springserver.domain.post.review.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.review.entity.PostReview
import java.time.LocalDateTime

@Repository
interface PostReviewRepository: JpaRepository<PostReview, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): PostReview?

    fun findByContract(
        contract: Contract
    ): PostReview?

    fun countByRevieweeIdAndDeletedAtIsNull(
        revieweeId: Long
    ): Long

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<PostReview>

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
        value = """
        select pr
        from PostReview pr
        join fetch pr.member
        where pr.reviewee.id = :revieweeId
          and pr.deletedAt is null
        order by pr.createdAt desc
        """,
        countQuery = """
        select count(pr)
        from PostReview pr
        where pr.reviewee.id = :revieweeId
          and pr.deletedAt is null
        """
    )
    fun findActiveReviewsByRevieweeId(
        @Param("revieweeId") revieweeId: Long,
        pageable: Pageable
    ): Page<PostReview>

    @Query(
        """
        select avg(pr.rating)
        from PostReview pr
        where pr.reviewee.id = :revieweeId
          and pr.deletedAt is null
        """
    )
    fun findAverageRatingByRevieweeId(
        @Param("revieweeId") revieweeId: Long
    ): Double?

    /**
     * 게시글이 보관 기간 만료로 하드 삭제될 때 리뷰까지 지우면 전문가 평판이
     * 게시글 수명에 묶인다. 리뷰는 남기고 게시글 참조만 끊는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update PostReview pr
        set pr.post = null
        where pr.post in :posts
        """
    )
    fun detachFromPosts(
        @Param("posts") posts: Collection<Post>
    ): Int
}
