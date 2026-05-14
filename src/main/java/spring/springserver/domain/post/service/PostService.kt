package spring.springserver.domain.post.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
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

    fun createPost(createRequest: CreatePostRequest): Long {

        val post = Post(
            title = createRequest.title,
            content = createRequest.content,
            image_url = createRequest.image_url,
            created_at = LocalDateTime.now(),
            updated_at = null
        )

        post.prePersist()

        return postRepository.save(post).id!!
    }

    fun findPostById(id: Long): PostResponse {

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
        post.image_url = updateRequest.image_url

        post.preUpdate()

        return PostResponse.of(post)
    }

    fun deletePost(id: Long) {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(AuthStatusCode.INVALID_CREDENTIALS)

        postRepository.delete(post)
    }
}
