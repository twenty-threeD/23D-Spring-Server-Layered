package spring.springserver.domain.community.job.data.response

import spring.springserver.domain.community.job.entity.CommunityJobPost
import spring.springserver.domain.community.job.entity.JobPostType
import spring.springserver.domain.community.job.repository.CommunityJobCommentRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostLikeRepository
import java.time.LocalDateTime

/**
 * 구인/구직 게시글 응답.
 * 일반 커뮤니티 글과 담는 값이 달라 CommunityPostResponse와 따로 둔다.
 */
data class CommunityJobPostResponse(
    val id: Long?,

    val username: String,

    /**
     * HIRING(구인) 또는 SEEKING(구직).
     */
    val postType: JobPostType,

    val title: String,

    val content: String?,

    val fileUrl: String?,

    val viewCount: Int,

    val isEdited: Boolean,

    val commentCount: Long,

    val likeCount: Long,

    /**
     * 요청한 회원이 이 글에 좋아요를 눌렀는지. 비로그인 조회에서는 false다.
     */
    val isLiked: Boolean,

    val jobCategoryId: Long?,

    val jobCategoryName: String,

    val sigCd: String,

    val sigKorNm: String?,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        /**
         * 단건 조회 전용. 글 하나마다 count 쿼리 두 개를 날리므로 목록에서 쓰지 않는다.
         * 목록은 서비스에서 집계를 한 번에 조회한 뒤 of()를 직접 호출한다.
         */
        fun toJobPostResponse(
            communityJobPost: CommunityJobPost,
            communityJobCommentRepository: CommunityJobCommentRepository,
            communityJobPostLikeRepository: CommunityJobPostLikeRepository,
            isLiked: Boolean
        ): CommunityJobPostResponse {

            val postId = communityJobPost.getId()!!

            return of(
                communityJobPost = communityJobPost,
                commentCount = communityJobCommentRepository
                    .countByCommunityJobPostIdAndDeletedAtIsNull(postId),
                likeCount = communityJobPostLikeRepository
                    .countByCommunityJobPostId(postId),
                isLiked = isLiked
            )
        }

        fun of(
            communityJobPost: CommunityJobPost,
            commentCount: Long,
            likeCount: Long,
            isLiked: Boolean
        ): CommunityJobPostResponse {

            val jobCategory = communityJobPost.jobCategory
            val sig = communityJobPost.sig

            return CommunityJobPostResponse(
                communityJobPost.getId(),
                communityJobPost.username,
                communityJobPost.postType,
                communityJobPost.title,
                communityJobPost.content,
                communityJobPost.fileUrl,
                communityJobPost.viewCount,
                communityJobPost.isEdited,
                commentCount,
                likeCount,
                isLiked,
                jobCategory.getId(),
                jobCategory.getFullName(),
                sig.getSigCd(),
                sig.sigKorNm,
                communityJobPost.getUpdatedAt(),
            )
        }
    }
}
