package spring.springserver.domain.post.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.post.dto.request.CreatePostRequest
import spring.springserver.domain.post.dto.request.UpdatePostRequest
import spring.springserver.domain.post.dto.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional
class PostService (private val postRepository: PostRepository) {

    fun createPost(createPostRequest: CreatePostRequest): PostResponse {

        val post = Post(
            title = createPostRequest.title,

            content = createPostRequest.content,

            updated_at = LocalDateTime.now(),

            member = createPostRequest.member
        )

        post.prePersist()

        return PostResponse.of(postRepository.save(post))
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        return PostResponse.of(post)
    }

    fun updatePost(
        id: Long,
        updateRequest: UpdatePostRequest): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        post.title = updateRequest.title
        post.content = updateRequest.content

        post.preUpdate()

        post.is_updated = true

        return PostResponse.of(post)
    }

    fun deletePost(id: Long) {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        post.is_deleted = true
        postRepository.delete(post)
    }
}
