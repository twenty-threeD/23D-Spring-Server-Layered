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
@Table(name = "community_post")
class CommunityPost(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 100)
    var username: String,

    @Column(length = 255, nullable = false)
    var title: String,

    @Column(length = 2000)
    var content: String?,

    @Column(length = 500)
    var fileUrl: String?,

    @Column(nullable = false)
    var viewCount: Int = 0,

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

    fun update(title: String, content: String?, fileUrl: String?) {
        this.title = title
        this.content = content
        this.fileUrl = fileUrl
        this.isEdited = true
    }

    fun increaseViewCount() {
        viewCount += 1
    }

    fun softDelete(now: LocalDateTime) {
        deletedAt = now
    }
}
