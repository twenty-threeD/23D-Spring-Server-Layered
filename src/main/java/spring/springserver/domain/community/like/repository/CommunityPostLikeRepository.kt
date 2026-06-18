package spring.springserver.domain.community.like.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.community.like.entity.CommunityPostLike
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member

interface CommunityPostLikeRepository : JpaRepository<CommunityPostLike, Long> {

    fun existsByMemberAndCommunityPost(
        member: Member,
        communityPost: CommunityPost
    ): Boolean

    fun countByCommunityPostId(
        communityPostId: Long
    ): Long

    fun deleteByMemberAndCommunityPost(
        member: Member,
        communityPost: CommunityPost
    ): Long

    fun deleteAllByCommunityPostIn(
        communityPosts: Collection<CommunityPost>
    )
}
