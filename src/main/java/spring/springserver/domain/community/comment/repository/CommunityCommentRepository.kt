package spring.springserver.domain.community.comment.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.community.comment.data.response.CommunityCommentResponse
import spring.springserver.domain.community.comment.entity.CommunityComment
import java.time.LocalDateTime

interface CommunityCommentRepository : JpaRepository<CommunityComment, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): CommunityComment?

    fun findAllByCommunityPostId(communityPostId: Long): List<CommunityComment>

    fun findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(communityPostId: Long): List<CommunityComment>

    fun countByCommunityPostIdAndDeletedAtIsNull(communityPostId: Long): Long

    fun findAllByDeletedAtBefore(deletedAt: LocalDateTime): List<CommunityComment>

    fun findAllByCommunityPostIdIn(communityPostIds: Collection<Long>): List<CommunityComment>
}
