package spring.springserver.domain.community.like.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.community.comment.entity.CommunityComment
import spring.springserver.domain.community.like.entity.CommunityCommentLike
import spring.springserver.domain.member.entity.Member

interface CommunityCommentLikeRepository : JpaRepository<CommunityCommentLike, Long> {

    fun existsByMemberAndCommunityComment(member: Member,
                                          communityComment: CommunityComment): Boolean

    fun countByCommunityCommentId(communityCommentId: Long): Long

    fun deleteByMemberAndCommunityComment(member: Member,
                                          communityComment: CommunityComment): Long

    fun deleteAllByCommunityCommentIn(communityComments: Collection<CommunityComment>)
}
