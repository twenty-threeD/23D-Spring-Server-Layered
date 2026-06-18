package spring.springserver.domain.post.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import spring.springserver.domain.auth.exception.AuthStatusCode
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
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class PostServiceImpl (
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val fileService: FileService
): PostService {

    override fun createPost(
        createPostRequest: CreatePostRequest
    ): PostResponse {

        val member = getCurrentMember()
        val post = createPostRequest.toEntity(member)

        createPostRequest.fileUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { fileUrl -> post.addAttachment(fileUrl) }

        return PostResponse.of(postRepository.save(post))
    }

    override fun viewPost(
        id: Long
    ): PostResponse {

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

    override fun viewAllPosts(
    ): List<PostResponse> {

        return postRepository.findAllByIsDeletedFalseOrderByUpdatedAtDesc()
            .map { post -> PostResponse.of(post) }
    }

    override fun searchPostsByTitle(
        title: String
    ): List<PostResponse> {

        val normalizedTitle = title.trim()

        return postRepository.searchPostsByTitle(normalizedTitle)
            .map { post -> PostResponse.of(post) }
    }

    override fun updatePost(
        updatePostRequest: UpdatePostRequest
    ): PostResponse {

        val post = postRepository.findPostById(updatePostRequest.id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.title = updatePostRequest.title

        post.content = updatePostRequest.content

        updatePostRequest.fileUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { fileUrl -> replaceAttachment(post, fileUrl) }

        post.preUpdate()

        post.isEdited = true

        return PostResponse.of(post)
    }

    override fun deletePost(
        id: Long
    ): DeletedPostResponse {

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

    private fun validatePostAuthor(
        post: Post
    ) {

        val member = getCurrentMember()

        if (post.member.getId() != member.getId()) {

            throw ApplicationException(PostStatusCode.FORBIDDEN_POST_ACCESS)
        }
    }

    private fun getCurrentMember() =
        SecurityContextHolder.getContext().authentication?.name
            ?.takeIf { username -> username.isNotBlank() && username != "anonymousUser" }
            ?.let { username -> memberRepository.findByUsername(username) }
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

    private fun replaceAttachment(
        post: Post,
        fileUrl: String
    ) {

        val oldFileUrls = post.attachments
            .mapNotNull { attachment -> attachment.fileUrl }

        if (oldFileUrls.size == 1 && oldFileUrls.first() == fileUrl) {

            return
        }

        post.attachments.clear()
        post.addAttachment(fileUrl)

        registerAttachedFileCommitCleanup(oldFileUrls)
    }

    private fun registerAttachedFileCommitCleanup(
        fileUrls: List<String>
    ) {

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