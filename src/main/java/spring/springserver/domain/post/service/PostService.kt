package spring.springserver.domain.post.service

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional
class PostService (
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository
) {

    fun createPost(createPostRequest: CreatePostRequest): PostResponse {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        val post = Post(
            title = createPostRequest.title,

            content = createPostRequest.content,

            updatedAt = LocalDateTime.now(),

            member = member
        )

        prePersist(post)

        return PostResponse.of(postRepository.save(post))
    }

    fun findPost(id: Long): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        post.viewCount += 1

        return PostResponse.of(post)
    }

    fun updatePost(updatePostRequest: UpdatePostRequest): PostResponse {

        val post = postRepository.findPostById(updatePostRequest.id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_POST)
        validatePostAuthor(post)

        post.title = updatePostRequest.title
        post.content = updatePostRequest.content

        preUpdate(post)

        post.isUpdated = true

        return PostResponse.of(post)
    }

    fun deletePost(id: Long) : PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_POST)
        validatePostAuthor(post)

        post.isDeleted = true

        return PostResponse.of(post)
    }

    @PrePersist
    fun prePersist(post: Post) {

        post.updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate(post: Post) {

        post.updatedAt = LocalDateTime.now()
    }

    private fun validatePostAuthor(post: Post) {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        if (post.member?.getId() != member.getId()) {
            throw ApplicationException(AuthStatusCode.FORBIDDEN_POST_ACCESS)
        }
    }
}
