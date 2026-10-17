package spring.springserver.domain.community.comment.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.comment.entity.CommunityComment
import java.time.LocalDateTime

interface CommunityCommentRepository : JpaRepository<CommunityComment, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityComment?

    fun findAllByCommunityPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        communityPostId: Long
    ): List<CommunityComment>

    fun countByCommunityPostIdAndDeletedAtIsNull(
        communityPostId: Long
    ): Long

    fun findAllByDeletedAtBefore(
        deletedAt: LocalDateTime
    ): List<CommunityComment>

    fun findAllByCommunityPostIdIn(
        communityPostIds: Collection<Long>
    ): List<CommunityComment>

    /**
     * 게시글 하나의 댓글·답글을 한 번에 전부 가져온다. 삭제 상태는 쿼리에 걸지 않고
     * 서비스에서 "답글이 있는 삭제된 원댓글" 처리를 하기 쉽게 모두 돌려준다.
     * N+1 을 피하려고 parent 와 member 를 함께 fetch 한다.
     */
    @Query(
        """
        select c
        from CommunityComment c
        left join fetch c.parent
        left join fetch c.member
        where c.communityPost.id = :postId
        order by c.createdAt asc
        """
    )
    fun findAllForPostWithParent(
        @Param("postId") postId: Long
    ): List<CommunityComment>

    /**
     * 원댓글을 지울 때 답글이 하나라도 남아 있는지 확인한다.
     * 답글은 자신이 삭제되면 바로 hard delete 되므로 조건은 parentId 만 보면 된다.
     */
    fun existsByParentId(
        parentId: Long
    ): Boolean
}
