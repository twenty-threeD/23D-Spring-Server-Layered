package spring.springserver.domain.community.post.data.response

import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.community.post.entity.CommunityPostType
import java.time.LocalDateTime

data class CommunityPostResponse(
    val id: Long?,

    val username: String,

    val title: String,

    val content: String?,

    val fileUrl: String?,

    val viewCount: Int,

    val isEdited: Boolean,

    val commentCount: Long,

    val likeCount: Long,

    /**
     * 일반 커뮤니티 글이면 GENERAL, 구인/구직 글이면 HIRING 또는 SEEKING.
     */
    val postType: CommunityPostType,

    /**
     * 아래 네 값은 구인/구직 글에만 채워진다.
     */
    val jobCategoryId: Long?,

    val jobCategoryName: String?,

    val sigCd: String?,

    val sigKorNm: String?,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun toPostResponse(
            communityPost: CommunityPost,
            communityCommentRepository: CommunityCommentRepository,
            communityPostLikeRepository: CommunityPostLikeRepository
        ): CommunityPostResponse {

            val postId = communityPost.getId()!!

            return of(
                communityPost = communityPost,
                commentCount = communityCommentRepository
                    .countByCommunityPostIdAndDeletedAtIsNull(postId),
                likeCount = communityPostLikeRepository
                    .countByCommunityPostId(postId)
            )
        }

        fun of(
            communityPost: CommunityPost,
            commentCount: Long,
            likeCount: Long
        ): CommunityPostResponse {

            val jobCategory = communityPost.jobCategory
            val sig = communityPost.sig

            return CommunityPostResponse(
                communityPost.getId(),
                communityPost.username,
                communityPost.title,
                communityPost.content,
                communityPost.fileUrl,
                communityPost.viewCount,
                communityPost.isEdited,
                commentCount,
                likeCount,
                communityPost.postType,
                jobCategory?.getId(),
                jobCategory?.getFullName(),
                sig?.getSigCd(),
                sig?.sigKorNm,
                communityPost.getUpdatedAt(),
            )
        }
    }
}
