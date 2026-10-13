package spring.springserver.domain.post.review.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 전문가 한 명이 받은 리뷰를 AI로 분석한 결과다.
 *
 * 평점과 마찬가지로 기준점은 게시글이 아니라 `reviewee`(전문가)다.
 * `analyzedReviewCount`는 분석에 사용한 리뷰 수이며, 현재 활성 리뷰 수와
 * 다를 때만 다시 분석한다. 같으면 저장된 결과를 그대로 돌려줘서
 * 조회마다 외부 API를 호출하지 않는다.
 */
@Entity
@Table(
    name = "post_review_analysis",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_review_analysis_reviewee",
            columnNames = ["reviewee_id"]
        )
    ]
)
class PostReviewAnalysis(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    val reviewee: Member,

    /**
     * 리뷰 전체를 요약한 한줄 총평.
     */
    @Column(nullable = false, length = 500)
    var summary: String,

    /**
     * 반복 언급되는 특성을 짧은 태그로 뽑은 목록.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "post_review_analysis_keyword",
        joinColumns = [JoinColumn(name = "analysis_id")]
    )
    @Column(name = "keyword", nullable = false, length = 50)
    var keywords: MutableList<String>,

    /**
     * 이 결과를 만들 때 사용한 활성 리뷰 수. 재분석 판단 기준이다.
     */
    @Column(name = "analyzed_review_count", nullable = false)
    var analyzedReviewCount: Long,
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
        summary: String,
        keywords: List<String>,
        analyzedReviewCount: Long
    ) {

        this.summary = summary
        this.keywords = keywords.toMutableList()
        this.analyzedReviewCount = analyzedReviewCount
    }
}
