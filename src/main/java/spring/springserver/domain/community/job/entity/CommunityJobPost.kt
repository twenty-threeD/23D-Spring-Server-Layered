package spring.springserver.domain.community.job.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.location.entity.Sig
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 용역 구인/구직 게시글.
 *
 * 일반 커뮤니티 글(community_post)과 요구하는 형식이 달라 테이블을 따로 쓴다.
 * 덕분에 카테고리·지역을 nullable로 열어두지 않고 컬럼 차원에서 not null로 묶을 수 있고,
 * 목록 조회에서 게시판을 가려내는 조건도 필요 없다.
 */
@Entity
@Table(name = "community_job_post")
class CommunityJobPost(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    @Column(nullable = false, length = 100)
    var username: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    var postType: JobPostType,

    @Column(length = 255, nullable = false)
    var title: String,

    @Column(length = 2000)
    var content: String?,

    @Column(length = 500)
    var fileUrl: String?,

    /**
     * 구인/구직 글의 용역 카테고리.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_category_id", nullable = false)
    var jobCategory: JobCategory,

    /**
     * 글이 걸린 시군구. 거리 기반 필터와 알림의 기준점이다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sig_cd", nullable = false)
    var sig: Sig,

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

    @UpdateTimestamp
    private var updatedAt: LocalDateTime? = null

    fun getId(): Long? = id

    fun getUpdatedAt(): LocalDateTime? = updatedAt

    fun update(
        postType: JobPostType,
        title: String,
        content: String?,
        fileUrl: String?,
        jobCategory: JobCategory,
        sig: Sig
    ) {

        this.postType = postType
        this.title = title
        this.content = content
        this.fileUrl = fileUrl
        this.jobCategory = jobCategory
        this.sig = sig
        this.isEdited = true
    }

    fun increaseViewCount() {

        viewCount += 1
    }

    fun softDelete(
        now: LocalDateTime
    ) {

        deletedAt = now
    }
}
