package spring.springserver.domain.community.post.data.response

import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.entity.Category
import spring.springserver.domain.community.post.entity.CommunityPost
import spring.springserver.domain.member.entity.Member
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

            // username은 작성 시점 값이 그대로 남아 있어 탈퇴 후에도 실명이 보인다.
            // 표시용 이름·프로필 이미지는 회원의 현재 상태를 기준으로 낮춘다.
            val isWithdrawn = communityPost.member.isDeleted()

            return CommunityPostResponse(
                communityPost.getId(),
                if (isWithdrawn) Member.WITHDRAWN_DISPLAY_NAME else communityPost.username,
                if (isWithdrawn) null else imageUrl,
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
