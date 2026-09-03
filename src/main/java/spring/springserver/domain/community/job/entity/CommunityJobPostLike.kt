package spring.springserver.domain.community.job.entity

import jakarta.persistence.*
import spring.springserver.domain.member.entity.Member

/**
 * 구인/구직 게시글 좋아요.
 * 한 회원이 같은 글에 두 번 누를 수 없도록 (member, post) 조합에 유니크 제약을 둔다.
 */
@Entity
@Table(
    name = "community_job_post_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_community_job_post_like_member_post",
            columnNames = ["member_id", "community_job_post_id"]
        )
    ]
)
class CommunityJobPostLike(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_job_post_id", nullable = false)
    val communityJobPost: CommunityJobPost,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId(): Long? = id
}
