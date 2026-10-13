package spring.springserver.domain.post.review.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.review.client.ReviewAnalysisClient
import spring.springserver.domain.post.review.data.response.PostReviewAnalysisResponse
import spring.springserver.domain.post.review.entity.PostReviewAnalysis
import spring.springserver.domain.post.review.exception.PostReviewStatusCode
import spring.springserver.domain.post.review.repository.PostReviewAnalysisRepository
import spring.springserver.domain.post.review.repository.PostReviewAnalysisWriter
import spring.springserver.domain.post.review.repository.PostReviewRepository
import spring.springserver.domain.post.review.service.PostReviewAnalysisService
import spring.springserver.global.exception.exception.ApplicationException
import java.time.Duration
import java.time.LocalDateTime

/**
 * 클래스 레벨에 @Transactional을 붙이지 않는다.
 *
 * Claude 호출이 수 초 걸리는데 그 구간을 트랜잭션에 넣으면 DB 커넥션을 붙잡고
 * 기다리게 되고, 동시 요청이 몰리면 커넥션 풀이 마른다. 조회는 리포지토리 메서드의
 * 자체 트랜잭션으로, 쓰기는 `PostReviewAnalysisWriter`의 짧은 트랜잭션으로 처리한다.
 */
@Service
class PostReviewAnalysisServiceImpl(
    private val postReviewAnalysisRepository: PostReviewAnalysisRepository,
    private val postReviewRepository: PostReviewRepository,
    private val memberRepository: MemberRepository,
    private val reviewAnalysisClient: ReviewAnalysisClient,
    private val postReviewAnalysisWriter: PostReviewAnalysisWriter
): PostReviewAnalysisService {

    companion object {

        /**
         * 리뷰가 너무 적으면 요약이 원문과 다를 바 없고, 작성자 특정도 쉬워진다.
         */
        private const val MIN_REVIEWS_TO_ANALYZE = 3L

        /**
         * 리뷰 수만 보고 재분석하면 리뷰를 쓰고 지우기를 반복해 유료 호출을 계속 유발할 수 있다.
         * 직전 분석이 이 시간 안이면 리뷰 수가 달라도 저장본을 그대로 돌려준다.
         */
        private val REANALYZE_COOLDOWN: Duration = Duration.ofHours(1)
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

        if (savedAnalysis != null && !needsReanalysis(
                savedAnalysis,
                reviewCount
            )
        ) {

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

        /**
         * 같은 전문가의 첫 분석이 동시에 들어오면 한쪽이 유니크 제약에 걸린다.
         * 그 트랜잭션은 롤백됐으니 새 트랜잭션으로 한 번 더 부르면 상대가 넣은 행을 갱신한다.
         */
        val analysis = try {
            postReviewAnalysisWriter.upsert(
                reviewee,
                reviewAnalysisResult,
                reviewCount
            )
        } catch (exception: DataIntegrityViolationException) {

            postReviewAnalysisWriter.upsert(
                reviewee,
                reviewAnalysisResult,
                reviewCount
            )
        }

        return toResponse(
            memberId,
            analysis
        )
    }

    /**
     * 리뷰 수가 그대로면 내용도 대체로 그대로다. 달라졌더라도 쿨다운 안이면 다시 분석하지 않는다.
     * 리뷰 수정만 일어난 경우는 잡지 못하지만, 그 비용을 감수한다.
     */
    private fun needsReanalysis(
        postReviewAnalysis: PostReviewAnalysis,
        reviewCount: Long
    ): Boolean {

        if (postReviewAnalysis.analyzedReviewCount == reviewCount) {

            return false
        }

        val analyzedAt = postReviewAnalysis.getUpdatedAt()
            ?: postReviewAnalysis.getCreatedAt()

        return analyzedAt.isBefore(LocalDateTime.now().minus(REANALYZE_COOLDOWN))
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
