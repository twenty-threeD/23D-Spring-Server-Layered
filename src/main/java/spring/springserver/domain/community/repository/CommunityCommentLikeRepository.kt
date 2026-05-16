package spring.springserver.domain.community.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.community.entity.CommunityComment
import spring.springserver.domain.community.entity.CommunityCommentLike
import spring.springserver.domain.member.entity.Member

interface CommunityCommentLikeRepository : JpaRepository<CommunityCommentLike, Long> {

    fun existsByMemberAndCommunityComment(member: Member, communityComment: CommunityComment): Boolean

    fun countByCommunityCommentId(communityCommentId: Long): Long

    fun deleteByMemberAndCommunityComment(member: Member, communityComment: CommunityComment): Long

    fun deleteAllByCommunityCommentIn(communityComments: Collection<CommunityComment>)
}
