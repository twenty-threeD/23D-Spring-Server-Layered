package spring.springserver.domain.community.comment.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(
    name = "community_comment",
    indexes = [
        /**
         * 글 상세의 댓글 목록과 댓글 수 집계가 탄다.
         */
        Index(name = "idx_community_comment_post", columnList = "community_post_id"),

        Index(name = "idx_community_comment_member", columnList = "member_id"),

        /**
         * 특정 원댓글의 답글을 뽑거나 원댓글 삭제 시 자식 유무를 확인하는 데 쓴다.
         */
        Index(name = "idx_community_comment_parent", columnList = "parent_id")
    ]
)
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

    /**
     * 답글이면 원댓글을 가리킨다. 깊이는 1단계만 허용하므로 parent 가 또 parent 를 가지면 안 된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: CommunityComment? = null
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

    fun isDeleted(): Boolean = deletedAt != null

    fun isReply(): Boolean = parent != null

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