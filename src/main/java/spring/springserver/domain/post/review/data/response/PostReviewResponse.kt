package spring.springserver.domain.post.review.data.response

import spring.springserver.domain.post.data.response.PostMemberResponse
import spring.springserver.domain.post.review.entity.PostReview
import java.time.LocalDateTime

data class PostReviewResponse(
    val id: Long?,

    val contractId: Long?,

    /**
     * 계약의 출발점이 된 게시글. 게시글 없이 맺은 계약이거나 게시글이
     * 보관 기간 만료로 삭제되면 null이다.
     */
    val postId: Long?,

    val rating: Int,

    val content: String,

    val isEdited: Boolean,

    /**
     * 리뷰 작성자(의뢰인).
     */
    val member: PostMemberResponse,

    /**
     * 평가 대상(전문가)의 회원 아이디.
     */
    val revieweeId: Long?,

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
                postReview.contract.getId(),
                postReview.post?.getId(),
                postReview.rating,
                postReview.content,
                postReview.isEdited,
                PostMemberResponse.of(
                    postReview.member,
                    memberImageUrl
                ),
                postReview.reviewee.getId(),
                postReview.getCreatedAt(),
                postReview.getUpdatedAt(),
            )
        }
    }
}
