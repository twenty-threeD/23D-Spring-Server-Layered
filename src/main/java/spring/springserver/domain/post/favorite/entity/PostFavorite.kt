package spring.springserver.domain.post.favorite.entity

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
import spring.springserver.domain.post.entity.Post

@Entity
@Table(
    name = "post_favorite",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_favorite_member_post",
            columnNames = ["member_id", "post_id"]
        )
    ]
)
class PostFavorite(
    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    fun getId(): Long? = id
}
