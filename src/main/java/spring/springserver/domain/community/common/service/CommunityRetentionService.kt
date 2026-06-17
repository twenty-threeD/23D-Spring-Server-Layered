package spring.springserver.domain.community.common.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityCommentLikeRepository
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityRetentionService(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityCommentLikeRepository: CommunityCommentLikeRepository
) {

    companion object {

        private const val RETENTION_DAYS = 30L
    }

    @Scheduled(cron = "0 0 4 * * *")
    fun purgeSoftDeletedContents() {

        val threshold = LocalDateTime.now().minusDays(RETENTION_DAYS)

        val expiredComments = communityCommentRepository.findAllByDeletedAtBefore(threshold)

        if (expiredComments.isNotEmpty()) {

            communityCommentLikeRepository.deleteAllByCommunityCommentIn(expiredComments)
            communityCommentRepository.deleteAll(expiredComments)
        }

        val expiredPosts = communityPostRepository.findAllByDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            val postIds = expiredPosts.mapNotNull { it.getId() }

            val commentsOfPosts = communityCommentRepository.findAllByCommunityPostIdIn(postIds)

            if (commentsOfPosts.isNotEmpty()) {

                communityCommentLikeRepository.deleteAllByCommunityCommentIn(commentsOfPosts)
                communityCommentRepository.deleteAll(commentsOfPosts)
            }

            communityPostRepository.deleteAll(expiredPosts)
        }
    }
}
