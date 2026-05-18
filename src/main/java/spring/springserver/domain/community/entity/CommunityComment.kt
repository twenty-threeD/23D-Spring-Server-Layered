package spring.springserver.domain.community.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(name = "community_comment")
class CommunityComment(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_post_id", nullable = false)
    var communityPost: CommunityPost,

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

    @CreationTimestamp
    private var createdAt: LocalDateTime? = null

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun getCreatedAt(): LocalDateTime? = createdAt

    fun getUpdatedAt(): LocalDateTime? = updatedAt

    fun update(content: String) {
        this.content = content
        this.isEdited = true
    }

    fun softDelete(now: LocalDateTime) {
        deletedAt = now
    }
}
