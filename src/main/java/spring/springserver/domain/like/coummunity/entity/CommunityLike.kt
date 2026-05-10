package spring.springserver.domain.like.coummunity.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import spring.springserver.domain.member.entity.Member

@Entity
@Table(
    name = "likes",
//    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "community_id"])]
)
class CommunityLike(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member

//    ,
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "community_id", nullable = false)
//    val community: Community
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId() = id
}