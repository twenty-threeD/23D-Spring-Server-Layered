CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- PostRepository.searchPostsByTitle / searchPostsByTitleAndCategoryIds
--   조건식: lower(p.title) like lower('%키워드%')
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_post_title_trgm
ON post USING gin (lower(title) gin_trgm_ops);

-- CommunityJobPostRepository.searchJobPostIds
--   조건식: coalesce(lower(p.title), '') like ... (content, username도 같은 형태)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_title_trgm
ON community_job_post USING gin (coalesce(lower(title), '') gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_content_trgm
ON community_job_post USING gin (coalesce(lower(content), '') gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_community_job_post_username_trgm
ON community_job_post USING gin (coalesce(lower(username), '') gin_trgm_ops);package spring.springserver.domain.community.post.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

/**
 * 일반 커뮤니티 게시글.
 * 구인/구직 글은 형식이 달라 community_job_post 테이블을 따로 쓰므로,
 * 이 테이블에는 게시판을 가려내기 위한 구분 값이 필요 없다.
 */
@Entity
@Table(
    name = "community_post",
    indexes = [
        /**
         * 목록·검색 쿼리가 모두 `deleted_at is null` + `updated_at desc`로 끝난다.
         */
        Index(
            name = "idx_community_post_not_deleted_updated_at",
            columnList = "deleted_at, updated_at"
        ),

        /**
         * 작성자별 글 목록과 회원 탈퇴 정리가 탄다.
         */
        Index(name = "idx_community_post_member", columnList = "member_id")
    ]
)
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
