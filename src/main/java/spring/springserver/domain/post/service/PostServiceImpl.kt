package spring.springserver.domain.post.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
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
class PostServiceImpl (private val postRepository: PostRepository,
                       private val memberRepository: MemberRepository) : PostService{


    override fun createPost(createPostRequest: CreatePostRequest): PostResponse {

        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApplicationException(AuthStatusCode.AVAILABLE_ACCESS_TOKEN)

        val member = memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        val post = Post(
            title = createPostRequest.title,

            content = createPostRequest.content,

            updatedAt = LocalDateTime.now(),

            member = member
        )

        return PostResponse.of(postRepository.save(post))
    }

    override fun findPost(id: Long): PostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        postRepository.incrementViewCount(id)

        val updatedPost = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        return PostResponse.of(updatedPost)
    }

    override fun updatePost(updatePostRequest: UpdatePostRequest): PostResponse {

        val post = postRepository.findPostById(updatePostRequest.id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        if (post.isDeleted) {

            throw ApplicationException(PostStatusCode.INVALID_POST)
        }

        validatePostAuthor(post)

        post.title = updatePostRequest.title
        post.content = updatePostRequest.content

        preUpdate(post)

        post.isUpdated = true

        return PostResponse.of(post)
    }

    override fun deletePost(id: Long) : DeletedPostResponse {

        val post = postRepository.findPostById(id)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)

        validatePostAuthor(post)

        post.isDeleted = true

        return DeletedPostResponse.of("삭제되었습니다")
    }

    fun preUpdate(post: Post) {

        post.updatedAt = LocalDateTime.now()
    }

    private fun validatePostAuthor(post: Post) {
        
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw ApplicationException(AuthStatusCode.AVAILABLE_ACCESS_TOKEN)

        val memberId = memberRepository.findByUsername(username)?.getId()
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)

        if (post.member?.getId() != memberId) {

            throw ApplicationException(PostStatusCode.FORBIDDEN_POST_ACCESS)
        }
    }
}
