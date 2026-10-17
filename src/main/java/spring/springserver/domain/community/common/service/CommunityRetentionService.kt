package spring.springserver.domain.community.common.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.community.comment.repository.CommunityCommentRepository
import spring.springserver.domain.community.like.repository.CommunityPostLikeRepository
import spring.springserver.domain.community.post.repository.CommunityPostRepository
import java.time.LocalDateTime

/**
 * 소프트 삭제된 커뮤니티 콘텐츠를 보관 기간이 지나면 실제로 지운다.
 */
@Service
@Transactional(rollbackFor = [Exception::class])
class CommunityRetentionService(
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository,
    private val communityPostLikeRepository: CommunityPostLikeRepository
) {

    companion object {

        private const val RETENTION_DAYS = 30L
    }

    @Scheduled(cron = "0 0 4 * * *")
    fun purgeSoftDeletedContents() {

        val threshold = LocalDateTime.now().minusDays(RETENTION_DAYS)

        purgeGeneralContents(threshold)
    }

    private fun purgeGeneralContents(
        threshold: LocalDateTime
    ) {

        val expiredComments = communityCommentRepository.findAllPurgeableBefore(threshold)

        if (expiredComments.isNotEmpty()) {

            communityCommentRepository.deleteAll(expiredComments)
        }

        val expiredPosts = communityPostRepository.findAllByDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            val postIds = expiredPosts.mapNotNull { it.getId() }

            val commentsOfPosts = communityCommentRepository.findAllByCommunityPostIdIn(postIds)

            if (commentsOfPosts.isNotEmpty()) {

                /**
                 * 답글이 원댓글을 parent_id 로 참조하므로 답글을 먼저 지운다.
                 */
                val (replies, rootComments) = commentsOfPosts.partition { comment -> comment.isReply() }

                communityCommentRepository.deleteAll(replies)
                communityCommentRepository.flush()
                communityCommentRepository.deleteAll(rootComments)
            }

            communityPostLikeRepository.deleteAllByCommunityPostIn(expiredPosts)
            communityPostRepository.deleteAll(expiredPosts)
        }
    }
}