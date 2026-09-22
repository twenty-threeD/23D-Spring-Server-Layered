package spring.springserver.domain.post.entity

import jakarta.persistence.*
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@Table(
    name = "post",
    indexes = [
        /**
         * 목록 조회(`findAllByIsDeletedFalseOrderByUpdatedAtDesc`)의 필터와 정렬을 함께 덮는다.
         */
        Index(name = "idx_post_not_deleted_updated_at", columnList = "is_deleted, updated_at")
    ]
)
class Post(
    @Column(nullable = false, length = 255)
    var title: String,

    @Column(length = 2000)
    var content: String,

    var viewCount: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime,

    var isEdited: Boolean = false,

    var isDeleted: Boolean = false,

    var deletedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_member_id", nullable = false)
    var member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    var category: JobCategory? = null,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachments: MutableList<PostAttach> = mutableListOf()
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    fun getId() = id

    fun preUpdate() {

        this.updatedAt = LocalDateTime.now()
    }

    fun addAttachment(
        fileUrl: String
    ) {

        attachments.add(
            PostAttach(
                fileUrl = fileUrl,
                post = this
            )
        )
    }
}
