package spring.springserver.domain.post.retention

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.file.service.FileService
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.repository.PostRepository
import java.time.LocalDateTime

@Component
class PostRetentionService(
    private val postRepository: PostRepository,
    private val fileService: FileService,
) {

    companion object {

        private const val RETENTION_DAYS = 30L
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(rollbackFor = [Exception::class])
    fun purgeSoftDeletedContents() {

        val threshold = LocalDateTime.now().minusDays(RETENTION_DAYS)

        val expiredPosts = postRepository.findAllByIsDeletedTrueAndDeletedAtBefore(threshold)

        if (expiredPosts.isNotEmpty()) {

            deleteAttachedFiles(expiredPosts)
            postRepository.deleteAll(expiredPosts)
        }
    }

    private fun deleteAttachedFiles(posts: List<Post>) {

        posts.flatMap { post -> post.attachments }
            .mapNotNull { attachment -> attachment.fileUrl }
            .forEach { fileUrl -> fileService.deleteFile(fileUrl) }
    }
}
