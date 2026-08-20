package spring.springserver.domain.post.review.data.response

import spring.springserver.domain.post.data.response.PostMemberResponse
import spring.springserver.domain.post.review.entity.PostReview
import java.time.LocalDateTime

data class PostReviewResponse(
    val id: Long?,

    val postId: Long?,

    val rating: Int,

    val content: String,

    val isEdited: Boolean,

    val member: PostMemberResponse,

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime?,
) {

    companion object {

        fun of(
            postReview: PostReview,
            memberImageUrl: String?
        ): PostReviewResponse {

            return PostReviewResponse(
                postReview.getId(),
                postReview.post.getId(),
                postReview.rating,
                postReview.content,
                postReview.isEdited,
                PostMemberResponse.of(
                    postReview.member,
                    memberImageUrl
                ),
                postReview.getCreatedAt(),
                postReview.getUpdatedAt(),
            )
        }
    }
}