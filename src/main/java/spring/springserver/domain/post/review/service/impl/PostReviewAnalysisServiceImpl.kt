package spring.springserver.domain.post.review.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.review.client.ReviewAnalysisClient
import spring.springserver.domain.post.review.data.response.PostReviewAnalysisResponse
import spring.springserver.domain.post.review.entity.PostReviewAnalysis
import spring.springserver.domain.post.review.exception.PostReviewStatusCode
import spring.springserver.domain.post.review.repository.PostReviewAnalysisRepository
import spring.springserver.domain.post.review.repository.PostReviewRepository
import spring.springserver.domain.post.review.service.PostReviewAnalysisService
import spring.springserver.domain.post.review.service.PostReviewAnalysisWriter
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.RedisLockExecutor
import java.time.Duration

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
    private val postRepository: PostRepository,
    private val reviewAnalysisClient: ReviewAnalysisClient,
    private val postReviewAnalysisWriter: PostReviewAnalysisWriter,
    private val redisLockExecutor: RedisLockExecutor
): PostReviewAnalysisService {

    companion object {

        /**
         * 리뷰가 너무 적으면 요약이 원문과 다를 바 없고, 작성자 특정도 쉬워진다.
         */
        private const val MIN_REVIEWS_TO_ANALYZE = 3L

        /**
         * 처음 분석한 뒤에는 리뷰가 이만큼 더 쌓였을 때만 다시 분석한다.
         * 리뷰 하나가 늘 때마다 다시 부르면 요약은 거의 그대로인데 유료 호출만 늘고,
         * 쓰고 지우기를 반복해 호출을 유발하는 것도 막기 어렵다.
         */
        private const val REANALYZE_REVIEW_COUNT_STEP = 5L

        /**
         * Claude 호출 최악 지연(timeout x 재시도)보다 넉넉해야 분석 중에 잠금이 풀리지 않는다.
         */
        private val ANALYSIS_LOCK_TTL: Duration = Duration.ofSeconds(60)

        private const val ANALYSIS_LOCK_KEY_PREFIX = "lock:post-review-analysis:"
    }

    override fun viewReviewAnalysis(
        postId: Long
    ): PostReviewAnalysisResponse {

        val post = getPost(postId)

        val reviewCount = postReviewRepository.countByPostIdAndDeletedAtIsNull(postId)

        if (reviewCount < MIN_REVIEWS_TO_ANALYZE) {

            throw ApplicationException(PostReviewStatusCode.NOT_ENOUGH_REVIEWS_TO_ANALYZE)
        }

        val savedAnalysis = postReviewAnalysisRepository.findByPostId(postId)

        if (savedAnalysis != null && !needsReanalysis(
                savedAnalysis,
                reviewCount
            )
        ) {

            return toResponse(
                postId,
                savedAnalysis
            )
        }

        /**
         * 글 상세를 여러 사람이 동시에 열면 전부 "저장본이 없다"고 보고 각자 유료 호출을 한다.
         * 게시글 단위로 잠금을 걸어 한 요청만 분석하고, 나머지는 저장본으로 돌려보낸다.
         */
        val analysis = redisLockExecutor.runWithLock(
            key = ANALYSIS_LOCK_KEY_PREFIX + postId,
            ttl = ANALYSIS_LOCK_TTL
        ) {
            analyzeAndSave(
                post,
                postId,
                reviewCount
            )
        }
            ?: savedAnalysis
            ?: throw ApplicationException(PostReviewStatusCode.REVIEW_ANALYSIS_IN_PROGRESS)

        return toResponse(
            postId,
            analysis
        )
    }

    private fun analyzeAndSave(
        post: Post,
        postId: Long,
        reviewCount: Long
    ): PostReviewAnalysis {

        /**
         * 잠금을 기다리는 동안 앞선 요청이 분석을 끝냈을 수 있다. 다시 확인해 중복 호출을 막는다.
         */
        val savedAnalysis = postReviewAnalysisRepository.findByPostId(postId)

        if (savedAnalysis != null && !needsReanalysis(
                savedAnalysis,
                reviewCount
            )
        ) {

            return savedAnalysis
        }

        val postReviews = postReviewRepository.findRecentActiveReviewsByPostId(
            postId,
            PageRequest.of(
                0,
                ReviewAnalysisClient.MAX_REVIEWS_IN_PROMPT
            )
        )

        val reviewAnalysisResult = reviewAnalysisClient.analyze(postReviews)

        /**
         * 잠금은 Redis TTL로 풀릴 수 있어 유니크 제약 충돌 가능성이 남는다.
         * 그 트랜잭션은 롤백됐으니 새 트랜잭션으로 한 번 더 부르면 상대가 넣은 행을 갱신한다.
         */
        return try {
            postReviewAnalysisWriter.upsert(
                post,
                reviewAnalysisResult,
                reviewCount
            )
        } catch (exception: DataIntegrityViolationException) {

            postReviewAnalysisWriter.upsert(
                post,
                reviewAnalysisResult,
                reviewCount
            )
        }
    }

    /**
     * 최소 조건을 채운 시점에 한 번 분석하고, 그 뒤로는 분석에 쓴 리뷰 수보다
     * `REANALYZE_REVIEW_COUNT_STEP`만큼 더 쌓였을 때만 다시 분석한다.
     * 리뷰가 지워져 수가 줄거나 내용만 수정된 경우는 잡지 못하지만, 그 비용을 감수한다.
     */
    private fun needsReanalysis(
        postReviewAnalysis: PostReviewAnalysis,
        reviewCount: Long
    ): Boolean {

        return reviewCount - postReviewAnalysis.analyzedReviewCount >= REANALYZE_REVIEW_COUNT_STEP
    }

    private fun toResponse(
        postId: Long,
        postReviewAnalysis: PostReviewAnalysis
    ): PostReviewAnalysisResponse {

        return PostReviewAnalysisResponse.of(
            postId,
            postReviewAnalysis.summary,
            postReviewAnalysis.keywords.toList(),
            postReviewAnalysis.analyzedReviewCount,
            postReviewAnalysis.getUpdatedAt()
        )
    }

    private fun getPost(
        postId: Long
    ): Post {

        val post = postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        return post
    }
}
