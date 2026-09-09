package spring.springserver.domain.community.job.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.job.entity.CommunityJobComment
import java.time.LocalDateTime

interface CommunityJobCommentRepository : JpaRepository<CommunityJobComment, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityJobComment?

    /**
     * 작성자 이름을 응답에 담으므로 member를 함께 가져온다.
     * ToOne 연관이라 페이징과 함께 fetch해도 행이 늘지 않는다.
     */
    @EntityGraph(attributePaths = ["member"])
    fun findAllByCommunityJobPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        communityJobPostId: Long,
        pageable: Pageable
    ): Page<CommunityJobComment>

    fun countByCommunityJobPostIdAndDeletedAtIsNull(
        communityJobPostId: Long
    ): Long

    /**
     * 목록용 댓글 수 집계. 글마다 count 쿼리를 날리지 않기 위해 한 번에 가져온다.
     */
    @Query(
        """
        select c.communityJobPost.id as postId, count(c.id) as count
        from CommunityJobComment c
        where c.communityJobPost.id in :postIds
          and c.deletedAt is null
        group by c.communityJobPost.id
        """
    )
    fun countCommentsByPostIds(
        @Param("postIds") postIds: Collection<Long>
    ): List<PostCountProjection>

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityJobComment>

    fun findAllByCommunityJobPostIdIn(
        communityJobPostIds: Collection<Long>
    ): List<CommunityJobComment>
}
