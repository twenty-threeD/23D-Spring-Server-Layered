package spring.springserver.domain.community.common.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.job.repository.CommunityJobCommentRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostLikeRepository
import spring.springserver.domain.community.job.repository.CommunityJobPostRepository
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import java.time.LocalDateTime

/**
 * 소프트 삭제된 커뮤니티 콘텐츠를 보관 기간이 지나면 실제로 지운다.
 * 게시판이 둘로 나뉘어 있으므로 일반 커뮤니티와 구인/구직을 각각 정리한다.
 */
@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityRetentionService(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityPostLikeRepository: CommunityPostLikeRepository,
    private val communityJobPostRepository: CommunityJobPostRepository,
    private val communityJobCommentRepository: CommunityJobCommentRepository,
    private val communityJobPostLikeRepository: CommunityJobPostLikeRepository
) {

    companion object {

        private const val RETENTION_DAYS = 30L
    }

    @Scheduled(cron = "0 0 4 * * *")
    fun purgeSoftDeletedContents() {

        val threshold = LocalDateTime.now().minusDays(RETENTION_DAYS)

        purgeGeneralContents(threshold)
        purgeJobContents(threshold)
    }

    private fun purgeGeneralContents(
        threshold: LocalDateTime
    ) {

        val expiredComments = communityCommentRepository.findAllByDeletedAtBefore(threshold)

        if (expiredComments.isNotEmpty()) {

            communityCommentRepository.deleteAll(expiredComments)
        }

        val expiredPosts = communityPostRepository.findAllByDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            val postIds = expiredPosts.mapNotNull { it.getId() }

            val commentsOfPosts = communityCommentRepository.findAllByCommunityPostIdIn(postIds)

            if (commentsOfPosts.isNotEmpty()) {

                communityCommentRepository.deleteAll(commentsOfPosts)
            }

            communityPostLikeRepository.deleteAllByCommunityPostIn(expiredPosts)
            communityPostRepository.deleteAll(expiredPosts)
        }
    }

    private fun purgeJobContents(
        threshold: LocalDateTime
    ) {

        val expiredComments = communityJobCommentRepository.findAllByDeletedAtBefore(threshold)

        if (expiredComments.isNotEmpty()) {

            communityJobCommentRepository.deleteAll(expiredComments)
        }

        val expiredPosts = communityJobPostRepository.findAllByDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            val postIds = expiredPosts.mapNotNull { it.getId() }

            val commentsOfPosts = communityJobCommentRepository
                .findAllByCommunityJobPostIdIn(postIds)

            if (commentsOfPosts.isNotEmpty()) {

                communityJobCommentRepository.deleteAll(commentsOfPosts)
            }

            communityJobPostLikeRepository.deleteAllByCommunityJobPostIn(expiredPosts)
            communityJobPostRepository.deleteAll(expiredPosts)
        }
    }
}
