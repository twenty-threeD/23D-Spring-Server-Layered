package spring.springserver.domain.community.comment.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.community.comment.entity.CommunityComment
import java.time.LocalDateTime

interface CommunityCommentRepository : JpaRepository<CommunityComment, Long> {

    fun findByIdAndDeletedAtIsNull(
        id: Long
    ): CommunityComment?

    fun countByCommunityPostIdAndDeletedAtIsNull(
        communityPostId: Long
    ): Long

    fun findAllByCommunityPostIdIn(
        communityPostIds: Collection<Long>
    ): List<CommunityComment>

    /**
     * 게시글의 최상위 댓글만 페이징한다. 삭제된 원댓글은 살아 있는 답글이 있을 때만 placeholder 로 포함한다.
     * member 는 fetch join 으로 묶어 댓글마다 조회하지 않게 한다.
     */
    @Query(
        value = """
        select c
        from CommunityComment c
        join fetch c.member
        where c.communityPost.id = :postId
          and c.parent is null
          and (
            c.deletedAt is null
            or exists (
              select 1
              from CommunityComment r
              where r.parent = c
                and r.deletedAt is null
            )
          )
        order by c.createdAt asc, c.id asc
        """,
        countQuery = """
        select count(c)
        from CommunityComment c
        where c.communityPost.id = :postId
          and c.parent is null
          and (
            c.deletedAt is null
            or exists (
              select 1
              from CommunityComment r
              where r.parent = c
                and r.deletedAt is null
            )
          )
        """
    )
    fun findRootCommentsByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): Page<CommunityComment>

    /**
     * 한 페이지에 실린 원댓글들의 답글을 한 번에 가져온다.
     */
    @Query(
        """
        select c
        from CommunityComment c
        join fetch c.member
        left join fetch c.replyToMember
        where c.parent.id in :parentIds
          and c.deletedAt is null
        order by c.createdAt asc, c.id asc
        """
    )
    fun findRepliesByParentIds(
        @Param("parentIds") parentIds: Collection<Long>
    ): List<CommunityComment>

    /**
     * 보관 기간이 지난 삭제 댓글 중 답글이 매달리지 않은 것만 고른다.
     * 답글이 남은 원댓글을 지우면 parent_id 외래키에 걸리므로 답글이 모두 사라진 뒤에 지운다.
     */
    @Query(
        """
        select c
        from CommunityComment c
        where c.deletedAt < :threshold
          and not exists (
            select 1
            from CommunityComment r
            where r.parent = c
          )
        """
    )
    fun findAllPurgeableBefore(
        @Param("threshold") threshold: LocalDateTime
    ): List<CommunityComment>

    /**
     * 원댓글을 지울 때 답글이 하나라도 남아 있는지 확인한다.
     * 답글은 자신이 삭제되면 바로 hard delete 되므로 조건은 parentId 만 보면 된다.
     */
    fun existsByParentId(
        parentId: Long
    ): Boolean
}
