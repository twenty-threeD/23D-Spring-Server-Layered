package spring.springserver.domain.community.post.data.response

import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import java.time.LocalDateTime

/**
 * 일반 커뮤니티 게시글 응답.
 * 구인/구직 글은 카테고리·지역까지 담아야 해서 CommunityJobPostResponse를 따로 쓴다.
 */
data class CommunityPostResponse(
    val id: Long?,

    val username: String,

    val imageUrl: String?,

    val title: String,

    val content: String?,

    val category: Category,

    val fileUrl: String?,

    val viewCount: Int,

    val isEdited: Boolean,

    val commentCount: Long,

    val likeCount: Long,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun toPostResponse(
            communityPost: CommunityPost,
            communityCommentRepository: CommunityCommentRepository,
            communityPostLikeRepository: CommunityPostLikeRepository,
            imageUrl: String?
        ): CommunityPostResponse {

            val postId = communityPost.getId()!!

            return of(
                communityPost = communityPost,
                commentCount = communityCommentRepository
                    .countByCommunityPostIdAndDeletedAtIsNull(postId),
                likeCount = communityPostLikeRepository
                    .countByCommunityPostId(postId),
                imageUrl = imageUrl
            )
        }

        fun of(
            communityPost: CommunityPost,
            commentCount: Long,
            likeCount: Long,
            imageUrl: String?
        ): CommunityPostResponse {

            return CommunityPostResponse(
                communityPost.getId(),
                communityPost.username,
                imageUrl,
                communityPost.title,
                communityPost.content,
                communityPost.category,
                communityPost.fileUrl,
                communityPost.viewCount,
                communityPost.isEdited,
                commentCount,
                likeCount,
                communityPost.getUpdatedAt(),
            )
        }
    }
}
