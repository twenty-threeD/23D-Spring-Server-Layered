package spring.springserver.domain.community.like.entity

import jakarta.persistence.*
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member

@Entity
@Table(
    name = "community_post_like",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_community_post_like_member_post", columnNames = ["member_id", "community_post_id"])
    ]
)
class CommunityPostLike(

    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_post_id", nullable = false)
    val communityPost: CommunityPost,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId(): Long? = id
}