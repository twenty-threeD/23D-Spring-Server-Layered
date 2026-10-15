package spring.springserver.domain.post.review.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.review.client.ReviewAnalysisClient
import spring.springserver.domain.post.review.client.ReviewAnalysisResult
import spring.springserver.domain.post.review.entity.PostReview
import spring.springserver.domain.post.review.entity.PostReviewAnalysis
import spring.springserver.domain.post.review.exception.PostReviewStatusCode
import spring.springserver.domain.post.review.repository.PostReviewAnalysisRepository
import spring.springserver.domain.post.review.repository.PostReviewRepository
import spring.springserver.domain.post.review.service.impl.PostReviewAnalysisServiceImpl
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.util.RedisLockExecutor
import java.time.Duration
import java.time.LocalDateTime

/**
 * AI 분석 결과를 언제 재사용하고 언제 다시 만드는지 검증한다.
 *
 * Claude 호출은 돈과 지연이 드는 경로라서, "리뷰 수가 그대로면, 혹은 쿨다운 안이면
 * 외부를 호출하지 않는다"가 이 서비스의 핵심 계약이다. 모델 응답 자체는 검증 대상이
 * 아니므로 클라이언트는 목으로 둔다.
 */
class PostReviewAnalysisServiceImplTest {

    private val postReviewAnalysisRepository = mock<PostReviewAnalysisRepository>()
    private val postReviewRepository = mock<PostReviewRepository>()
    private val postRepository = mock<PostRepository>()
    private val reviewAnalysisClient = mock<ReviewAnalysisClient>()
    private val postReviewAnalysisWriter = mock<PostReviewAnalysisWriter>()
    private val redisLockExecutor = mock<RedisLockExecutor>()

    private lateinit var postReviewAnalysisService: PostReviewAnalysisServiceImpl

    companion object {

        private const val POST_ID = 1L
    }

    @BeforeEach
    fun setUp() {

        postReviewAnalysisService = PostReviewAnalysisServiceImpl(
            postReviewAnalysisRepository,
            postReviewRepository,
            postRepository,
            reviewAnalysisClient,
            postReviewAnalysisWriter,
            redisLockExecutor
        )

        givenLockIsAcquired()

        whenever(postRepository.findPostById(POST_ID)).thenReturn(mock<Post>())
    }

    @Test
    @DisplayName("리뷰가 최소 기준보다 적으면 분석하지 않고 예외를 던진다")
    fun rejectsWhenReviewsAreNotEnough() {

        givenReviewCount(2L)

        assertThatThrownBy { postReviewAnalysisService.viewReviewAnalysis(POST_ID) }
            .isInstanceOf(ApplicationException::class.java)
            .hasFieldOrPropertyWithValue(
                "statusCode",
                PostReviewStatusCode.NOT_ENOUGH_REVIEWS_TO_ANALYZE
            )

        verify(reviewAnalysisClient, never()).analyze(any())
    }

    @Test
    @DisplayName("저장된 분석의 리뷰 수가 현재와 같으면 Claude를 호출하지 않고 저장본을 돌려준다")
    fun reusesSavedAnalysisWhenReviewCountIsUnchanged() {

        givenReviewCount(5L)

        givenSavedAnalysis(
            summary = "저장된 총평",
            keywords = listOf("시간 약속 정확"),
            analyzedReviewCount = 5L,
            analyzedAt = LocalDateTime.now().minusDays(30)
        )

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("저장된 총평")
        assertThat(response.keywords).containsExactly("시간 약속 정확")
        assertThat(response.analyzedReviewCount).isEqualTo(5L)

        verify(reviewAnalysisClient, never()).analyze(any())
        verify(postReviewRepository, never()).findRecentActiveReviewsByPostId(any(), any())
    }

    @Test
    @DisplayName("리뷰 수가 달라져도 쿨다운 안이면 재분석하지 않는다")
    fun skipsReanalysisWithinCooldown() {

        givenReviewCount(9L)

        givenSavedAnalysis(
            summary = "방금 만든 총평",
            keywords = listOf("태그"),
            analyzedReviewCount = 5L,
            analyzedAt = LocalDateTime.now().minusMinutes(5)
        )

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("방금 만든 총평")

        verify(reviewAnalysisClient, never()).analyze(any())
    }

    @Test
    @DisplayName("쿨다운이 지나고 리뷰가 늘어나면 다시 분석해 저장한다")
    fun reanalyzesWhenReviewCountGrewAfterCooldown() {

        givenReviewCount(7L)

        givenSavedAnalysis(
            summary = "오래된 총평",
            keywords = listOf("오래된 태그"),
            analyzedReviewCount = 5L,
            analyzedAt = LocalDateTime.now().minusHours(3)
        )

        givenReviews(3)

        givenAnalysisResult(
            summary = "새 총평",
            keywords = listOf("소통 원활", "결과물 만족")
        )

        givenWriterReturnsUpsertedAnalysis()

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("새 총평")
        assertThat(response.keywords).containsExactly("소통 원활", "결과물 만족")
        assertThat(response.analyzedReviewCount).isEqualTo(7L)

        verify(postReviewAnalysisWriter).upsert(
            any(),
            eq(ReviewAnalysisResult("새 총평", listOf("소통 원활", "결과물 만족"))),
            eq(7L)
        )
    }

    @Test
    @DisplayName("저장된 분석이 없으면 새로 만들어 저장한다")
    fun createsAnalysisWhenNoneSaved() {

        givenReviewCount(4L)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(null)

        givenReviews(4)

        givenAnalysisResult(
            summary = "첫 총평",
            keywords = listOf("꼼꼼함")
        )

        givenWriterReturnsUpsertedAnalysis()

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("첫 총평")
        assertThat(response.keywords).containsExactly("꼼꼼함")
        assertThat(response.analyzedReviewCount).isEqualTo(4L)
    }

    @Test
    @DisplayName("첫 분석이 동시에 들어와 유니크 제약에 걸리면 새 트랜잭션으로 한 번 더 저장한다")
    fun retriesUpsertOnUniqueViolation() {

        givenReviewCount(4L)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(null)

        givenReviews(4)

        givenAnalysisResult(
            summary = "총평",
            keywords = emptyList()
        )

        whenever(
            postReviewAnalysisWriter.upsert(
                any(),
                any(),
                any()
            )
        )
            .thenThrow(DataIntegrityViolationException("uk_post_review_analysis_post"))
            .thenReturn(
                PostReviewAnalysis(
                    mock<Post>(),
                    "총평",
                    mutableListOf(),
                    4L
                )
            )

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("총평")

        verify(postReviewAnalysisWriter, times(2)).upsert(
            any(),
            any(),
            any()
        )

        /**
         * 재시도는 저장만 다시 하고 유료 호출은 반복하지 않는다.
         */
        verify(reviewAnalysisClient, times(1)).analyze(any())
    }

    /**
     * GlobalExceptionHandler는 StatusCode의 HttpStatus를 그대로 응답에 붙이면서
     * 본문은 BaseResponse.error로 내린다. 2xx를 쓰면 프론트가 성공으로 읽고 data를 뒤져
     * 화면이 깨지므로, 실패 코드에 2xx가 섞이는 일을 막는다.
     */
    @Test
    @DisplayName("리뷰 분석 실패 상태 코드는 2xx를 쓰지 않는다")
    fun analysisFailureStatusCodesAreNotSuccessful() {

        listOf(
            PostReviewStatusCode.NOT_ENOUGH_REVIEWS_TO_ANALYZE,
            PostReviewStatusCode.REVIEW_ANALYSIS_FAILED,
            PostReviewStatusCode.REVIEW_ANALYSIS_IN_PROGRESS,
            PostReviewStatusCode.REVIEW_ANALYSIS_NOT_CONFIGURED
        ).forEach { statusCode ->

            assertThat(statusCode.getHttpStatus().isError)
                .`as`("%s", statusCode.getCode())
                .isTrue()
        }
    }

    @Test
    @DisplayName("잠금을 못 잡으면 Claude를 호출하지 않고 저장본을 돌려준다")
    fun returnsSavedAnalysisWhenLockIsHeld() {

        givenReviewCount(9L)

        givenSavedAnalysis(
            summary = "직전 총평",
            keywords = listOf("태그"),
            analyzedReviewCount = 5L,
            analyzedAt = LocalDateTime.now().minusHours(3)
        )

        givenLockIsNotAcquired()

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("직전 총평")

        verify(reviewAnalysisClient, never()).analyze(any())
    }

    @Test
    @DisplayName("잠금도 못 잡고 저장본도 없으면 분석 중으로 응답한다")
    fun rejectsWhenLockIsHeldAndNothingSaved() {

        givenReviewCount(4L)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(null)

        givenLockIsNotAcquired()

        assertThatThrownBy { postReviewAnalysisService.viewReviewAnalysis(POST_ID) }
            .isInstanceOf(ApplicationException::class.java)
            .hasFieldOrPropertyWithValue(
                "statusCode",
                PostReviewStatusCode.REVIEW_ANALYSIS_IN_PROGRESS
            )

        verify(reviewAnalysisClient, never()).analyze(any())
    }

    @Test
    @DisplayName("잠금을 잡은 뒤 앞선 요청이 이미 분석을 끝냈으면 다시 호출하지 않는다")
    fun rechecksSavedAnalysisAfterAcquiringLock() {

        givenReviewCount(7L)

        /**
         * 잠금 획득 전에는 저장본이 없고, 획득 후 재확인에서는 최신 분석이 보이는 상황.
         */
        val freshAnalysis = mock<PostReviewAnalysis>()

        whenever(freshAnalysis.summary).thenReturn("앞선 요청이 만든 총평")
        whenever(freshAnalysis.keywords).thenReturn(mutableListOf("태그"))
        whenever(freshAnalysis.analyzedReviewCount).thenReturn(7L)
        whenever(freshAnalysis.getUpdatedAt()).thenReturn(LocalDateTime.now())

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID))
            .thenReturn(null)
            .thenReturn(freshAnalysis)

        val response = postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        assertThat(response.summary).isEqualTo("앞선 요청이 만든 총평")

        verify(reviewAnalysisClient, never()).analyze(any())
        verify(postReviewAnalysisWriter, never()).upsert(any(), any(), any())
    }

    @Test
    @DisplayName("게시글 단위 키로 잠금을 건다")
    fun locksPerPost() {

        givenReviewCount(4L)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(null)

        givenReviews(4)

        givenAnalysisResult(
            summary = "총평",
            keywords = emptyList()
        )

        givenWriterReturnsUpsertedAnalysis()

        postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        val keyCaptor = argumentCaptor<String>()
        val ttlCaptor = argumentCaptor<Duration>()

        verify(redisLockExecutor).runWithLock(
            keyCaptor.capture(),
            ttlCaptor.capture(),
            any<() -> PostReviewAnalysis>()
        )

        assertThat(keyCaptor.firstValue).isEqualTo("lock:post-review-analysis:$POST_ID")
        assertThat(ttlCaptor.firstValue).isGreaterThanOrEqualTo(Duration.ofSeconds(40))
    }

    @Test
    @DisplayName("프롬프트에 넣는 리뷰는 상한 개수로 잘라서 조회한다")
    fun limitsReviewsSentToClaude() {

        givenReviewCount(500L)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(null)

        givenReviews(10)

        givenAnalysisResult(
            summary = "총평",
            keywords = emptyList()
        )

        givenWriterReturnsUpsertedAnalysis()

        postReviewAnalysisService.viewReviewAnalysis(POST_ID)

        val pageableCaptor = argumentCaptor<Pageable>()

        verify(postReviewRepository).findRecentActiveReviewsByPostId(
            eq(POST_ID),
            pageableCaptor.capture()
        )

        assertThat(pageableCaptor.firstValue.pageSize)
            .isEqualTo(ReviewAnalysisClient.MAX_REVIEWS_IN_PROMPT)
    }

    /**
     * 잠금을 잡은 요청만 분석한다. 잡지 못한 요청은 `runWithLock`이 null을 주므로
     * 저장본이 있으면 그걸, 없으면 "분석 중" 응답으로 끝나야 한다.
     */
    private fun givenLockIsAcquired() {

        whenever(
            redisLockExecutor.runWithLock(
                any(),
                any(),
                any<() -> PostReviewAnalysis>()
            )
        ).thenAnswer { invocation ->

            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[2] as () -> PostReviewAnalysis).invoke()
        }
    }

    private fun givenLockIsNotAcquired() {

        whenever(
            redisLockExecutor.runWithLock(
                any(),
                any(),
                any<() -> PostReviewAnalysis>()
            )
        ).thenReturn(null)
    }

    private fun givenReviewCount(
        reviewCount: Long
    ) {

        whenever(postReviewRepository.countByPostIdAndDeletedAtIsNull(POST_ID))
            .thenReturn(reviewCount)
    }

    /**
     * `createdAt`·`updatedAt`은 JPA가 채우는 값이라 세터가 없다. 쿨다운 분기를 태우려면
     * 분석 시점을 직접 정해야 하므로 엔티티를 목으로 세운다.
     */
    private fun givenSavedAnalysis(
        summary: String,
        keywords: List<String>,
        analyzedReviewCount: Long,
        analyzedAt: LocalDateTime
    ): PostReviewAnalysis {

        val savedAnalysis = mock<PostReviewAnalysis>()

        whenever(savedAnalysis.summary).thenReturn(summary)
        whenever(savedAnalysis.keywords).thenReturn(keywords.toMutableList())
        whenever(savedAnalysis.analyzedReviewCount).thenReturn(analyzedReviewCount)
        whenever(savedAnalysis.getUpdatedAt()).thenReturn(analyzedAt)
        whenever(savedAnalysis.getCreatedAt()).thenReturn(analyzedAt)

        whenever(postReviewAnalysisRepository.findByPostId(POST_ID)).thenReturn(savedAnalysis)

        return savedAnalysis
    }

    /**
     * 실제 upsert 동작은 PostReviewAnalysisWriter의 책임이라, 여기서는 넘겨받은 값을
     * 그대로 담은 엔티티를 돌려주는 선에서 끊는다.
     */
    private fun givenWriterReturnsUpsertedAnalysis() {

        whenever(
            postReviewAnalysisWriter.upsert(
                any(),
                any(),
                any()
            )
        ).thenAnswer { invocation ->

            val reviewAnalysisResult = invocation.arguments[1] as ReviewAnalysisResult

            PostReviewAnalysis(
                mock<Post>(),
                reviewAnalysisResult.summary,
                reviewAnalysisResult.keywords.toMutableList(),
                invocation.arguments[2] as Long
            )
        }
    }

    private fun givenReviews(
        reviewCount: Int
    ) {

        whenever(postReviewRepository.findRecentActiveReviewsByPostId(any(), any()))
            .thenReturn(List(reviewCount) { mock<PostReview>() })
    }

    private fun givenAnalysisResult(
        summary: String,
        keywords: List<String>
    ) {

        whenever(reviewAnalysisClient.analyze(any())).thenReturn(
            ReviewAnalysisResult(
                summary,
                keywords
            )
        )
    }
}
