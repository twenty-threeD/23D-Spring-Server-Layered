package spring.springserver.domain.post.review.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.contract.exception.ContractStatusCode
import spring.springserver.domain.contract.repository.ContractRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
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
import spring.springserver.domain.profile.service.ProfileService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime
import kotlin.math.round

@Service
@Transactional(rollbackFor = [Exception::class])
class PostReviewServiceImpl(
    private val postReviewRepository: PostReviewRepository,
    private val contractRepository: ContractRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val profileService: ProfileService
): PostReviewService {

    companion object {

        private const val MIN_RATING = 1
        private const val MAX_RATING = 5
    }

    override fun createReview(
        createPostReviewRequest: CreatePostReviewRequest
    ): PostReviewResponse {

        val member = getCurrentMember()

        val contract = contractRepository.findContractById(createPostReviewRequest.contractId)
            ?: throw ApplicationException(ContractStatusCode.CONTRACT_NOT_FOUND)

        validateRating(createPostReviewRequest.rating)

        validateClient(
            contract,
            member
        )

        val content = createPostReviewRequest.content.trim()

        val writtenReview = postReviewRepository.findByContract(contract)

        if (writtenReview != null) {

            if (writtenReview.deletedAt == null) throw ApplicationException(PostReviewStatusCode.ALREADY_REVIEWED_CONTRACT)

            writtenReview.rewrite(
                createPostReviewRequest.rating,
                content,
                LocalDateTime.now()
            )

            return toResponse(writtenReview)
        }

        val postReview = try {

            postReviewRepository.saveAndFlush(
                PostReview(
                    contract = contract,
                    member = contract.client,
                    reviewee = contract.professional,
                    post = contract.post,
                    rating = createPostReviewRequest.rating,
                    content = content,
                )
            )
        } catch (exception: DataIntegrityViolationException) {

            throw ApplicationException(PostReviewStatusCode.ALREADY_REVIEWED_CONTRACT)
        }

        return toResponse(postReview)
    }

    @Transactional(readOnly = true)
    override fun viewReviews(
        postId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse> {

        val post = postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        return toResponses(
            postReviewRepository.findActiveReviewsByPostId(
                postId,
                pageable.withoutSort()
            )
        )
    }

    @Transactional(readOnly = true)
    override fun viewMemberReviews(
        memberId: Long,
        pageable: Pageable
    ): Page<PostReviewResponse> {

        getMember(memberId)

        return toResponses(
            postReviewRepository.findActiveReviewsByRevieweeId(
                memberId,
                pageable.withoutSort()
            )
        )
    }

    @Transactional(readOnly = true)
    override fun viewReviewSummary(
        memberId: Long
    ): PostReviewSummaryResponse {

        getMember(memberId)

        val averageRating = postReviewRepository.findAverageRatingByRevieweeId(memberId)
            ?: 0.0

        return PostReviewSummaryResponse.of(
            memberId,
            postReviewRepository.countByRevieweeIdAndDeletedAtIsNull(memberId),
            round(averageRating * 10) / 10
        )
    }

    override fun updateReview(
        updatePostReviewRequest: UpdatePostReviewRequest
    ): PostReviewResponse {

        val member = getCurrentMember()

        val postReview = getActiveReview(updatePostReviewRequest.reviewId)

        validateRating(updatePostReviewRequest.rating)

        validateReviewAuthor(
            postReview,
            member
        )

        postReview.update(
            updatePostReviewRequest.rating,
            updatePostReviewRequest.content.trim()
        )

        return toResponse(postReview)
    }

    override fun deleteReview(
        reviewId: Long
    ): DeletedPostReviewResponse {

        val member = getCurrentMember()

        val postReview = getActiveReview(reviewId)

        validateReviewAuthor(
            postReview,
            member
        )

        postReview.softDelete(LocalDateTime.now())

        return DeletedPostReviewResponse.of("삭제되었습니다.")
    }

    private fun toResponses(
        postReviews: Page<PostReview>
    ): Page<PostReviewResponse> {

        val imageUrls = profileService.getImageUrlsByMemberIds(
            postReviews.content.mapNotNull { postReview -> postReview.member.getId() }
        )

        return postReviews.map { postReview ->

            PostReviewResponse.of(
                postReview,
                imageUrls[postReview.member.getId()]
            )
        }
    }

    private fun toResponse(
        postReview: PostReview
    ): PostReviewResponse {

        val memberId = postReview.member.getId()

        return PostReviewResponse.of(
            postReview,
            memberId?.let { id -> profileService.getImageUrlByMemberId(id) }
        )
    }

    private fun getCurrentMember() = SecurityContextHolder.getContext().authentication?.name
        ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
        ?.let { username -> memberRepository.findByUsername(username) }
        ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

    private fun getMember(
        memberId: Long
    ): Member {

        return memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
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

    /**
     * 리뷰는 계약의 의뢰인(갑)이 전문가(을)에게 남긴다. 전문가나 제3자가
     * 쓰려고 하면 막는다. 계약 자체가 거래의 증거이므로 별도 거래 검증은 없다.
     */
    private fun validateClient(
        contract: Contract,
        member: Member
    ) {

        if (!contract.isClient(member.getId())) {

            throw ApplicationException(PostReviewStatusCode.REVIEW_CLIENT_ONLY)
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
