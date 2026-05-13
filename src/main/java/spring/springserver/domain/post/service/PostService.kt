package spring.springserver.domain.post.service

import jakarta.transaction.Transactional
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import spring.springserver.domain.post.dto.request.CreatePostRequest
import spring.springserver.domain.post.dto.request.UpdatePostRequest
import spring.springserver.domain.post.dto.response.PostResponse
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.repository.PostRepository

@Service
class PostService (
    private val postRepository: PostRepository
){

    @Transactional
    fun createPost(createRequest: CreatePostRequest): Long {

        val post = Post(
            title = createRequest.title,
            content = createRequest.content,
            image_url = createRequest.image_url,
            status = createRequest.status,
            created_at = null
        )
        post.prePersist()

        return postRepository.save(post).id!!
    }

    @Transactional
    fun findPostById(id: Long): PostResponse {

        val post = postRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Postid not found") }
        post.view_count += 1

        return PostResponse.of(post)
    }

    @Transactional
    fun updatePost(id: Long, updateRequest: UpdatePostRequest): PostResponse {

        val post = postRepository.findById(id)
        .orElseThrow { IllegalArgumentException("Postid not found") }

        post.title = updateRequest.title
        post.content = updateRequest.content
        post.image_url = updateRequest.image_url
        post.status = updateRequest.status

        post.preUpdate()

        return PostResponse.of(post)
    }

    @Transactional
    fun deletePost(id: Long) {

        val post = postRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Postid not found") }

        postRepository.delete(post)
    }
}
