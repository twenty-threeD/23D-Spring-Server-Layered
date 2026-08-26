package spring.springserver.domain.post.review.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

@Entity
@Table(
    name = "post_review",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_review_member_post",
            columnNames = ["member_id", "post_id"]
        )
    ]
)
class PostReview(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(nullable = false)
    var rating: Int,

    @Column(nullable = false, length = 1000)
    var content: String,

    @Column(nullable = false)
    var isEdited: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @Column(name = "created_at", nullable = false)
    private var createdAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun getCreatedAt(): LocalDateTime = createdAt

    fun getUpdatedAt(): LocalDateTime? = updatedAt

    fun update(
        rating: Int,
        content: String
    ) {

        this.rating = rating
        this.content = content
        this.isEdited = true
    }

    fun rewrite(
        rating: Int,
        content: String,
        now: LocalDateTime
    ) {

        this.rating = rating
        this.content = content
        this.isEdited = false
        this.deletedAt = null
        this.createdAt = now
    }

    fun softDelete(
        now: LocalDateTime
    ) {

        deletedAt = now
    }
}