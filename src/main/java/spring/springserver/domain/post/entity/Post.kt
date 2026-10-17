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
        Index(name = "idx_post_not_deleted_updated_at", columnList = "is_deleted, updated_at"),

        /**
         * 작성자별 글 목록(`findAllWithDetailsByMemberUsername`)과 회원 삭제 정리가 탄다.
         */
        Index(name = "idx_post_client_member", columnList = "client_member_id"),

        /**
         * 카테고리 필터 검색(`searchPostsByTitleAndCategoryIds`)이 탄다.
         */
        Index(name = "idx_post_job_category", columnList = "job_category_id")
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
    var attachments: MutableList<PostAttach> = mutableListOf(),

    /**
     * 재구매율 집계. 결제 confirm/cancel 때 재계산되고,
     * 매 조회에서 계약·결제를 다시 집계하지 않도록 미리 저장한다.
     * 기존 행이 있는 운영 테이블에서 ddl-auto=update 가 ALTER TABLE ADD COLUMN 할 때
     * DB default 가 없으면 NOT NULL 추가가 거부되므로 columnDefinition 으로 default 를 박는다.
     */
    @Column(nullable = false, columnDefinition = "integer not null default 0")
    var buyerCount: Int = 0,

    @Column(nullable = false, columnDefinition = "integer not null default 0")
    var repeatBuyerCount: Int = 0,

    @Column(nullable = false, columnDefinition = "double precision not null default 0")
    var repeatRate: Double = 0.0
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

    fun applyRepeatStats(
        buyerCount: Int,
        repeatBuyerCount: Int,
        repeatRate: Double
    ) {

        this.buyerCount = buyerCount
        this.repeatBuyerCount = repeatBuyerCount
        this.repeatRate = repeatRate
    }
}
