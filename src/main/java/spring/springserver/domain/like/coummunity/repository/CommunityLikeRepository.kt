package spring.springserver.domain.like.coummunity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import spring.springserver.domain.like.coummunity.entity.CommunityLike
import spring.springserver.domain.member.entity.Member

@Repository
interface CommunityLikeRepository: JpaRepository<CommunityLike, Long>{

//    fun countByCommunityPostId(communityPostId: Long): Long

    fun findByMember(member: Member): CommunityLike?
//    fun findByMemberAndCommunityPost(member: Member, communityPost: CommunityPost): Like?

    fun existsByMember(member: Member): Boolean
//    fun existsByMemberAndCommunityPost(member: Member, communityPost: CommunityPost): Boolean

//    fun countByPost(post: Post): Long

    fun deleteByMember(member: Member)
//    fun deleteByMemberAndCommunityPost(member: Member, communityPost: CommunityPost)
}