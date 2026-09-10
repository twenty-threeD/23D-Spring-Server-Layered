package spring.springserver.domain.community.job.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import spring.springserver.domain.community.common.service.CommunityAuthorizationService
import spring.springserver.domain.community.job.data.request.JobPostLikeRequest
import spring.springserver.domain.community.job.data.response.JobPostLikeResponse
import spring.springserver.domain.community.job.entity.CommunityJobPostLike
import spring.springserver.domain.community.job.repository.CommunityJobPostLikeRepository
import spring.springserver.domain.community.job.service.CommunityJobAuthorizationService
import spring.springserver.domain.community.job.service.CommunityJobLikeService

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityJobLikeServiceImpl(
    private val communityAuthorizationService: CommunityAuthorizationService,
    private val communityJobAuthorizationService: CommunityJobAuthorizationService,
    private val communityJobPostLikeRepository: CommunityJobPostLikeRepository,
    private val platformTransactionManager: PlatformTransactionManager
): CommunityJobLikeService {

    private val requiresNewTransaction = TransactionTemplate(platformTransactionManager).apply {

        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * 이미 눌렀으면 에러 대신 현재 상태를 그대로 돌려준다.
     * 좋아요는 재시도·더블탭으로 같은 요청이 반복되기 쉬워, 중복을 실패로 다루면
     * 프론트가 성공/실패 두 갈래로 상태를 따로 관리해야 한다.
     */
    override fun likeJobPost(
        jobPostLikeRequest: JobPostLikeRequest
    ): JobPostLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val postId = jobPostLikeRequest.postId!!

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        if (!communityJobPostLikeRepository.existsByMemberAndCommunityJobPost(member, communityJobPost)) {

            /**
             * exists 확인과 INSERT 사이에 같은 회원의 요청이 겹치면 유니크 제약에 걸린다.
             * 결과적으로 좋아요가 눌린 상태는 동일하므로 실패로 보지 않는다.
             *
             * 반드시 별도 트랜잭션에서 저장한다. 같은 트랜잭션에서 제약 위반이 나면
             * 예외를 잡더라도 세션이 오염되고 rollback-only로 마킹되어,
             * 뒤따르는 조회가 정상 동작하는 것처럼 보여도 커밋에서 UnexpectedRollbackException이 난다.
             */
            runCatching {

                requiresNewTransaction.execute {

                    communityJobPostLikeRepository.save(
                        CommunityJobPostLike(
                            member = member,
                            communityJobPost = communityJobPost,
                        )
                    )
                }
            }.onFailure {

                throwable ->
                if (throwable !is DataIntegrityViolationException) {

                    throw throwable
                }
            }
        }

        return JobPostLikeResponse.of(
            postId = postId,
            likeCount = communityJobPostLikeRepository.countByCommunityJobPostId(postId),
            isLiked = true,
            message = "게시글 좋아요가 등록되었습니다.",
        )
    }

    /**
     * 누르지 않은 상태에서의 취소도 최종 상태가 같으므로 성공으로 본다.
     */
    override fun unlikeJobPost(
        postId: Long
    ): JobPostLikeResponse {

        val member = communityAuthorizationService.getCurrentMember()

        val communityJobPost = communityJobAuthorizationService.getActiveJobPost(postId)

        communityJobPostLikeRepository.deleteByMemberAndCommunityJobPost(
            member,
            communityJobPost
        )

        return JobPostLikeResponse.of(
            postId = postId,
            likeCount = communityJobPostLikeRepository.countByCommunityJobPostId(postId),
            isLiked = false,
            message = "게시글 좋아요가 취소되었습니다.",
        )
    }
}
