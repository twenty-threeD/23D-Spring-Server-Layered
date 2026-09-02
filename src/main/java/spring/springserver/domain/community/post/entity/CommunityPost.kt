package spring.springserver.domain.community.post.entity

import jakarta.persistence.*
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

    @Column(length = 2000)
    var fileUrl: String?,

    @Column(nullable = false)
    var viewCount: Int = 0,

    @Column(nullable = false)
    var isEdited: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: Category
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun getUpdatedAt(): LocalDateTime? = updatedAt

    fun update(
        title: String,
        content: String?,
        category: Category,
        fileUrl: String?
    ) {

        this.title = title
        this.content = content
        this.category = category
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