package spring.springserver.domain.post.review.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post
import java.time.LocalDateTime

/**
 * 계약 한 건에 대해 의뢰인(갑)이 전문가(을)에게 남기는 리뷰다.
 *
 * 리뷰의 기준점은 게시글이 아니라 계약이다. 게시글 기준으로 잡으면
 * 거래한 적 없는 사람도 평점을 남길 수 있고, 평가 대상이 글 주인(의뢰인)이 되어
 * 방향이 뒤집힌다. `post`는 게시글 화면에서 리뷰를 묶어 보여주기 위한
 * 비정규화 컬럼일 뿐이고, 평점 집계는 항상 `reviewee` 기준으로 한다.
 */
@Entity
@Table(
    name = "post_review",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_review_contract",
            columnNames = ["contract_id"]
        )
    ],
    indexes = [
        /**
         * 전문가 프로필의 평점·리뷰 수 집계가 reviewee_id 단독으로 조회한다.
         */
        Index(name = "idx_post_review_reviewee", columnList = "reviewee_id"),

        /**
         * 글 상세의 리뷰 목록이 post_id 단독으로 조회한다.
         */
        Index(name = "idx_post_review_post", columnList = "post_id")
    ]
)
class PostReview(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    val contract: Contract,

    /**
     * 리뷰 작성자. 계약의 의뢰인(갑)이다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    /**
     * 평가 대상. 계약의 전문가(을)이다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    val reviewee: Member,

    /**
     * 계약의 출발점이 된 게시글. 게시글 없이 맺은 계약이거나
     * 게시글이 보관 기간 만료로 삭제되면 null이 된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    var post: Post? = null,

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
    private var id: Long? = null

    @Column(name = "created_at", nullable = false)
    private var createdAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId() = id

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
