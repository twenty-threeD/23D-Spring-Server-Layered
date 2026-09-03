package spring.springserver.domain.community.job.repository

import org.springframework.data.jpa.repository.JpaRepository
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

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityJobComment>

    fun findAllByCommunityJobPostIdIn(
        communityJobPostIds: Collection<Long>
    ): List<CommunityJobComment>
}
