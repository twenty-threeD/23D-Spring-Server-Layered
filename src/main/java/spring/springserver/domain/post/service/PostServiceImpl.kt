package spring.springserver.domain.post.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.file.data.request.FileUploadRequest
import spring.springserver.domain.file.service.FileService
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.global.exception.exception.ApplicationException
import spring.springserver.global.jwt.MemberDetails
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class PostServiceImpl (
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val fileService: FileService
): PostService {

    override fun createPost(createPostRequest: CreatePostRequest,
                            multipartFile: MultipartFile?): PostResponse {

        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApplicationException(AuthStatusCode.AVAILABLE_ACCESS_TOKEN)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val post = createPostRequest.toEntity(member)

        if (multipartFile != null && !multipartFile.isEmpty) {

            val uploadResponse = fileService.uploadFile(FileUploadRequest(multipartFile))
            registerUploadedFileRollbackCleanup(uploadResponse.fileUrl())
            post.addAttachment(uploadResponse.fileUrl())
        }

        return PostResponse.of(postRepository.save(post))
    }

    override fun viewPost(id: Long): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        val updatePost = postRepository.incrementViewCount(id)

        if (updatePost == 0) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        val updatedPost = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        return PostResponse.of(updatedPost)
    }

    override fun updatePost(updatePostRequest: UpdatePostRequest,
                            multipartFile: MultipartFile?): PostResponse {

        val post = postRepository.findPostById(updatePostRequest.id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.title = updatePostRequest.title

        post.content = updatePostRequest.content

        if (multipartFile != null && !multipartFile.isEmpty) {

            replaceAttachment(post, multipartFile)
        }

        post.preUpdate(post)

        post.isEdited = true

        return PostResponse.of(post)
    }

    override fun deletePost(id: Long): DeletedPostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.isDeleted = true
        post.deletedAt = LocalDateTime.now()

        return DeletedPostResponse.of("삭제되었습니다")
    }

    private fun validatePostAuthor(post: Post) {

        val principal = SecurityContextHolder.getContext().authentication?.principal
                as? MemberDetails
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        if (post.member.getId() != principal.getId()) {

            throw ApplicationException(PostStatusCode.FORBIDDEN_POST_ACCESS)
        }
    }

    private fun replaceAttachment(post: Post, multipartFile: MultipartFile) {

        val oldFileUrls = post.attachments
            .mapNotNull { attachment -> attachment.fileUrl }

        val uploadResponse = fileService.uploadFile(FileUploadRequest(multipartFile))
        registerUploadedFileRollbackCleanup(uploadResponse.fileUrl())

        post.attachments.clear()
        post.addAttachment(uploadResponse.fileUrl())

        registerAttachedFileCommitCleanup(oldFileUrls)
    }

    private fun registerUploadedFileRollbackCleanup(fileUrl: String) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {

            override fun afterCompletion(status: Int) {

                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {

                    fileService.deleteFile(fileUrl)
                }
            }
        })
    }

    private fun registerAttachedFileCommitCleanup(fileUrls: List<String>) {

        if (fileUrls.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {

            return
        }

        TransactionSynchronizationManager.registerSynchronization(object: TransactionSynchronization {

            override fun afterCommit() {

                fileUrls.forEach { fileUrl -> fileService.deleteFile(fileUrl) }
            }
        })
    }
}
