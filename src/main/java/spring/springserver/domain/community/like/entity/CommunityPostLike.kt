package spring.springserver.domain.community.like.entity

import jakarta.persistence.*
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member

@Entity
@Table(
    name = "community_post_like",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_community_post_like_member_post", columnNames = ["member_id", "community_post_id"])
    ],
    indexes = [
        /**
         * 목록의 좋아요 수 집계가 글 id 단독으로 조회한다.
         */
        Index(name = "idx_community_post_like_post", columnList = "community_post_id")
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