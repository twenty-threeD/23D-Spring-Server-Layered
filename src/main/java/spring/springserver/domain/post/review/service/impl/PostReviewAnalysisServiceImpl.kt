package spring.springserver.domain.post.review.service.impl

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.review.client.ReviewAnalysisClient
import spring.springserver.domain.post.review.data.response.PostReviewAnalysisResponse
import spring.springserver.domain.post.review.entity.PostReviewAnalysis
import spring.springserver.domain.post.review.exception.PostReviewStatusCode
import spring.springserver.domain.post.review.repository.PostReviewAnalysisRepository
import spring.springserver.domain.post.review.repository.PostReviewRepository
import spring.springserver.domain.post.review.service.PostReviewAnalysisService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class PostReviewAnalysisServiceImpl(
    private val postReviewAnalysisRepository: PostReviewAnalysisRepository,
    private val postReviewRepository: PostReviewRepository,
    private val memberRepository: MemberRepository,
    private val reviewAnalysisClient: ReviewAnalysisClient
): PostReviewAnalysisService {

    companion object {

        /**
         * 리뷰가 너무 적으면 요약이 원문과 다를 바 없고, 작성자 특정도 쉬워진다.
         */
        private const val MIN_REVIEWS_TO_ANALYZE = 3L
    }

    override fun viewReviewAnalysis(
        memberId: Long
    ): PostReviewAnalysisResponse {

        val reviewee = getMember(memberId)

        val reviewCount = postReviewRepository.countByRevieweeIdAndDeletedAtIsNull(memberId)

        if (reviewCount < MIN_REVIEWS_TO_ANALYZE) {

            throw ApplicationException(PostReviewStatusCode.NOT_ENOUGH_REVIEWS_TO_ANALYZE)
        }

        val savedAnalysis = postReviewAnalysisRepository.findByRevieweeId(memberId)

        /**
         * 리뷰 수가 그대로면 내용도 대체로 그대로다. 저장본을 돌려줘서 외부 호출을 아낀다.
         * 리뷰 수정만 일어난 경우는 잡지 못하지만, 그 비용을 감수한다.
         */
        if (savedAnalysis != null && savedAnalysis.analyzedReviewCount == reviewCount) {

            return toResponse(
                memberId,
                savedAnalysis
            )
        }

        val postReviews = postReviewRepository.findRecentActiveReviewsByRevieweeId(
            memberId,
            PageRequest.of(
                0,
                ReviewAnalysisClient.MAX_REVIEWS_IN_PROMPT
            )
        )

        val reviewAnalysisResult = reviewAnalysisClient.analyze(postReviews)

        val analysis = savedAnalysis
            ?.also {
                it.update(
                    reviewAnalysisResult.summary,
                    reviewAnalysisResult.keywords,
                    reviewCount
                )
            }
            ?: postReviewAnalysisRepository.save(
                PostReviewAnalysis(
                    reviewee,
                    reviewAnalysisResult.summary,
                    reviewAnalysisResult.keywords.toMutableList(),
                    reviewCount
                )
            )

        return toResponse(
            memberId,
            analysis
        )
    }

    private fun toResponse(
        memberId: Long,
        postReviewAnalysis: PostReviewAnalysis
    ): PostReviewAnalysisResponse {

        return PostReviewAnalysisResponse.of(
            memberId,
            postReviewAnalysis.summary,
            postReviewAnalysis.keywords.toList(),
            postReviewAnalysis.analyzedReviewCount,
            postReviewAnalysis.getUpdatedAt()
        )
    }

    private fun getMember(
        memberId: Long
    ): Member {

        return memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
    }
}
