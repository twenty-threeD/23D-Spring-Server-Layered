package spring.springserver.domain.community.job.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 구인/구직 게시글의 댓글.
 * 게시글 테이블이 갈렸으므로 댓글도 community_job_post를 가리키는 전용 테이블을 쓴다.
 */
@Entity
@Table(name = "community_job_comment")
class CommunityJobComment(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_job_post_id", nullable = false)
    var communityJobPost: CommunityJobPost,

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

    fun update(
        content: String
    ) {

        this.content = content
        this.isEdited = true
    }

    fun softDelete(
        now: LocalDateTime
    ) {

        deletedAt = now
    }
}
