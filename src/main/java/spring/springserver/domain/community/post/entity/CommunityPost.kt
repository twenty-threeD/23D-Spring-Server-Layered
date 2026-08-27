package spring.springserver.domain.community.post.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.location.entity.Sig
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

    /**
     * 기존 커뮤니티 글은 전부 GENERAL이다.
     * 이미 쌓여 있는 행에도 값이 채워져야 하므로 컬럼 기본값을 함께 지정한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "post_type",
        nullable = false,
        length = 20,
        columnDefinition = "varchar(20) not null default 'GENERAL'"
    )
    var postType: CommunityPostType = CommunityPostType.GENERAL,

    /**
     * 구인/구직 글의 용역 카테고리. 일반 글에는 없다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    var jobCategory: JobCategory? = null,

    /**
     * 구인/구직 글이 걸린 시군구. 거리 기반 필터와 알림의 기준점이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sig_cd")
    var sig: Sig? = null,
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
        fileUrl: String?
    ) {

        this.title = title
        this.content = content
        this.fileUrl = fileUrl
        this.isEdited = true
    }

    /**
     * 구인/구직 글은 게시판 구분·카테고리·지역까지 함께 바뀔 수 있다.
     */
    fun updateJobPost(
        title: String,
        content: String?,
        fileUrl: String?,
        postType: CommunityPostType,
        jobCategory: JobCategory,
        sig: Sig
    ) {

        update(title, content, fileUrl)

        this.postType = postType
        this.jobCategory = jobCategory
        this.sig = sig
    }

    fun isJobPost(): Boolean = postType.isJobPost()

    fun increaseViewCount() {

        viewCount += 1
    }

    fun softDelete(now: LocalDateTime) {

        deletedAt = now
    }
}
