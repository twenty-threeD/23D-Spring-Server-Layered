package spring.springserver.domain.community.job.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.job.entity.CommunityJobComment
import java.time.LocalDateTime

interface CommunityJobCommentRepository : JpaRepository<CommunityJobComment, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityJobComment?

    fun findAllByCommunityJobPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        communityJobPostId: Long
    ): List<CommunityJobComment>

    fun countByCommunityJobPostIdAndDeletedAtIsNull(
        communityJobPostId: Long
    ): Long

    /**
     * 목록용 댓글 수 집계. 글마다 count 쿼리를 날리지 않기 위해 한 번에 가져온다.
     */
    @Query(
        """
        select c.communityJobPost.id, count(c.id)
        from CommunityJobComment c
        where c.communityJobPost.id in :postIds
          and c.deletedAt is null
        group by c.communityJobPost.id
        """
    )
    fun countCommentsByPostIds(
        @Param("postIds") postIds: Collection<Long>
    ): List<Array<Any>>

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityJobComment>

    fun findAllByCommunityJobPostIdIn(
        communityJobPostIds: Collection<Long>
    ): List<CommunityJobComment>
}
