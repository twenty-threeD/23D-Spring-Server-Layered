package spring.springserver.domain.community.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import spring.springserver.domain.member.entity.Member

@Entity
@Table(
    name = "community_comment_like",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_community_comment_like_member_comment", columnNames = ["member_id", "community_comment_id"])
    ]
)
class CommunityCommentLike(

    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_comment_id", nullable = false)
    val communityComment: CommunityComment,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId(): Long? = id
}
