package spring.springserver.domain.post.retention

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import spring.springserver.domain.file.service.FileService
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.favorite.repository.PostFavoriteRepository
import spring.springserver.domain.post.repository.PostRepository
import java.time.LocalDateTime

@Component
class PostRetentionService(
    private val postRepository: PostRepository,
    private val postFavoriteRepository: PostFavoriteRepository,
    private val fileService: FileService,
) {

    companion object {

        private val log = LoggerFactory.getLogger(PostRetentionService::class.java)
        private const val RETENTION_DAYS = 30L
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(rollbackFor = [Exception::class])
    fun purgeSoftDeletedContents() {

        val threshold = LocalDateTime.now().minusDays(RETENTION_DAYS)

        val expiredPosts = postRepository.findAllByIsDeletedTrueAndDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            registerAttachedFileCommitCleanup(expiredPosts)
            postFavoriteRepository.deleteAllByPostIn(expiredPosts)
            postRepository.deleteAll(expiredPosts)
        }
    }

    private fun registerAttachedFileCommitCleanup(
        posts: List<Post>
    ) {

        val fileUrls = posts.flatMap {

            post -> post.attachments
        }
            .mapNotNull {

                attachment -> attachment.fileUrl
            }

        if (fileUrls.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {

            override fun afterCommit() {

                fileUrls.forEach {

                    fileUrl ->

                    try {

                        fileService.deleteFile(fileUrl)
                    } catch (exception: Exception) {

                        log.warn("Failed to delete retained post attachment file. fileUrl={}", fileUrl, exception)
                    }
                }
            }
        })
    }
}