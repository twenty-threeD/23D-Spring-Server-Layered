package spring.springserver.domain.community.job.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.entity.CommunityJobPostLike
import spring.springserver.domain.member.entity.Member

interface CommunityJobPostLikeRepository : JpaRepository<CommunityJobPostLike, Long> {

    fun existsByMemberAndCommunityJobPost(
        member: Member,
        communityJobPost: CommunityJobPost
    ): Boolean

    fun countByCommunityJobPostId(
        communityJobPostId: Long
    ): Long

    fun deleteByMemberAndCommunityJobPost(
        member: Member,
        communityJobPost: CommunityJobPost
    ): Long

    fun deleteAllByCommunityJobPostIn(
        communityJobPosts: Collection<CommunityJobPost>
    )
}
