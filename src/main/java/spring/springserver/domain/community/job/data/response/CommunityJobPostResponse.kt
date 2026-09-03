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

    val jobCategoryId: Long?,

    val jobCategoryName: String,

    val sigCd: String,

    val sigKorNm: String?,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun toJobPostResponse(
            communityJobPost: CommunityJobPost,
            communityJobCommentRepository: CommunityJobCommentRepository,
            communityJobPostLikeRepository: CommunityJobPostLikeRepository
        ): CommunityJobPostResponse {

            val postId = communityJobPost.getId()!!

            return of(
                communityJobPost = communityJobPost,
                commentCount = communityJobCommentRepository
                    .countByCommunityJobPostIdAndDeletedAtIsNull(postId),
                likeCount = communityJobPostLikeRepository
                    .countByCommunityJobPostId(postId)
            )
        }

        fun of(
            communityJobPost: CommunityJobPost,
            commentCount: Long,
            likeCount: Long
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
                jobCategory.getId(),
                jobCategory.getFullName(),
                sig.getSigCd(),
                sig.sigKorNm,
                communityJobPost.getUpdatedAt(),
            )
        }
    }
}
