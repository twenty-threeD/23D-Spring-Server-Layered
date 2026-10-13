package spring.springserver.domain.post.review.repository

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.review.client.ReviewAnalysisResult
import spring.springserver.domain.post.review.entity.PostReviewAnalysis

/**
 * 분석 결과 저장만 담당한다.
 *
 * 서비스에서 분리한 이유는 트랜잭션 경계다. Claude 호출은 수 초가 걸리는데
 * 그 구간을 트랜잭션에 넣으면 DB 커넥션을 붙잡고 기다리게 된다. 그래서 서비스는
 * 트랜잭션 없이 흐르고, 쓰기만 이 컴포넌트의 짧은 트랜잭션으로 처리한다.
 */
@Component
class PostReviewAnalysisWriter(
    private val postReviewAnalysisRepository: PostReviewAnalysisRepository
) {

    /**
     * reviewee당 한 행만 두므로 없으면 넣고 있으면 갱신한다.
     *
     * 같은 전문가의 첫 분석이 동시에 들어오면 양쪽 모두 insert를 시도해
     * `uk_post_review_analysis_reviewee`에 걸린다. 제약 위반이 나면 이 트랜잭션은
     * 이미 롤백 대상이라 안에서 복구할 수 없다. 그래서 예외를 그대로 올리고,
     * 재시도는 호출자가 새 트랜잭션으로 한 번 더 부르는 식으로 처리한다.
     */
    @Transactional(rollbackFor = [Exception::class])
    fun upsert(
        reviewee: Member,
        reviewAnalysisResult: ReviewAnalysisResult,
        reviewCount: Long
    ): PostReviewAnalysis {

        val revieweeId = reviewee.getId()
            ?: throw IllegalArgumentException("저장되지 않은 회원으로는 분석 결과를 남길 수 없다.")

        val savedAnalysis = postReviewAnalysisRepository.findByRevieweeId(revieweeId)

        if (savedAnalysis != null) {

            savedAnalysis.update(
                reviewAnalysisResult.summary,
                reviewAnalysisResult.keywords,
                reviewCount
            )

            return savedAnalysis
        }

        /**
         * 제약 위반을 커밋 시점이 아니라 이 자리에서 받으려고 flush까지 한다.
         */
        return postReviewAnalysisRepository.saveAndFlush(
            PostReviewAnalysis(
                reviewee,
                reviewAnalysisResult.summary,
                reviewAnalysisResult.keywords.toMutableList(),
                reviewCount
            )
        )
    }
}
