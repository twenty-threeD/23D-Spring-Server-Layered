package spring.springserver.domain.post.review.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.review.data.request.CreatePostReviewRequest
import spring.springserver.domain.post.review.data.request.UpdatePostReviewRequest
import spring.springserver.domain.post.review.data.response.DeletedPostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewResponse
import spring.springserver.domain.post.review.data.response.PostReviewSummaryResponse
import spring.springserver.domain.post.review.entity.PostReview
import spring.springserver.domain.post.review.exception.PostReviewStatusCode
import spring.springserver.domain.post.review.repository.PostReviewRepository
import spring.springserver.domain.post.review.service.PostReviewService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime
import kotlin.math.round

@Service
@Transactional(rollbackFor = [Exception::class])
class PostReviewServiceImpl(
    private val postReviewRepository: PostReviewRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository
): PostReviewService {

    companion object {

        private const val MIN_RATING = 1
        private const val MAX_RATING = 5
    }

    override fun createReview(
        createPostReviewRequest: CreatePostReviewRequest
    ): PostReviewResponse {

        val member = getCurrentMember()

        val post = getActivePost(createPostReviewRequest.postId)

        validateRating(createPostReviewRequest.rating)

        validateNotPostAuthor(post, member)

        val content = createPostReviewRequest.content.trim()

        val writtenReview = postReviewRepository.findByMemberAndPost(member, post)

        if (writtenReview != null) {

            if (writtenReview.deletedAt == null) {

                throw ApplicationException(PostReviewStatusCode.ALREADY_REVIEWED_POST)
            }

            writtenReview.rewrite(
                createPostReviewRequest.rating,
                content,
                LocalDateTime.now()
            )

            return PostReviewResponse.of(writtenReview)
        }

        val postReview = try {

            postReviewRepository.saveAndFlush(
                PostReview(
                    member = member,
                    post = post,
                    rating = createPostReviewRequest.rating,
                    content = content,
                )
            )
        } catch (exception: DataIntegrityViolationException) {

            throw ApplicationException(PostReviewStatusCode.ALREADY_REVIEWED_POST)
        }

        return PostReviewResponse.of(postReview)
    }

    @Transactional(readOnly = true)
    override fun viewReviews(
        postId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse> {

        getActivePost(postId)

        return postReviewRepository.findActiveReviewsByPostId(
            postId,
            pageable.withoutSort()
        ).map { postReview -> PostReviewResponse.of(postReview) }
    }

    @Transactional(readOnly = true)
    override fun viewReviewSummary(
        postId: Long
    ): PostReviewSummaryResponse {

        getActivePost(postId)

        val averageRating = postReviewRepository.findAverageRatingByPostId(postId)
            ?: 0.0

        return PostReviewSummaryResponse.of(
            postId,
            postReviewRepository.countByPostIdAndDeletedAtIsNull(postId),
            round(averageRating * 10) / 10
        )
    }

    override fun updateReview(
        updatePostReviewRequest: UpdatePostReviewRequest
    ): PostReviewResponse {

        val member = getCurrentMember()

        val postReview = getActiveReview(updatePostReviewRequest.reviewId)

        validateRating(updatePostReviewRequest.rating)

        validateReviewAuthor(postReview, member)

        postReview.update(
            updatePostReviewRequest.rating,
            updatePostReviewRequest.content.trim()
        )

        return PostReviewResponse.of(postReview)
    }

    override fun deleteReview(
        reviewId: Long
    ): DeletedPostReviewResponse {

        val member = getCurrentMember()

        val postReview = getActiveReview(reviewId)

        validateReviewAuthor(postReview, member)

        postReview.softDelete(LocalDateTime.now())

        return DeletedPostReviewResponse.of("삭제되었습니다.")
    }

    private fun getCurrentMember() = SecurityContextHolder.getContext().authentication?.name
        ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
        ?.let { username -> memberRepository.findByUsername(username) }
        ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

    private fun getActivePost(
        postId: Long
    ): Post {

        val post = postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        return post
    }

    private fun getActiveReview(
        reviewId: Long
    ): PostReview {

        return postReviewRepository.findByIdAndDeletedAtIsNull(reviewId)
            ?: throw ApplicationException(PostReviewStatusCode.INVALID_POST_REVIEW)
    }

    private fun validateRating(
        rating: Int
    ) {

        if (rating < MIN_RATING || rating > MAX_RATING) {

            throw ApplicationException(PostReviewStatusCode.INVALID_POST_REVIEW_RATING)
        }
    }

    private fun validateNotPostAuthor(
        post: Post,
        member: Member
    ) {

        if (post.member.getId() == member.getId()) {

            throw ApplicationException(PostReviewStatusCode.SELF_POST_REVIEW_NOT_ALLOWED)
        }
    }

    private fun validateReviewAuthor(
        postReview: PostReview,
        member: Member
    ) {

        if (postReview.member.getId() != member.getId()) {

            throw ApplicationException(PostReviewStatusCode.FORBIDDEN_POST_REVIEW_ACCESS)
        }
    }

    private fun Pageable.withoutSort(): Pageable =
        PageRequest.of(pageNumber, pageSize)
}