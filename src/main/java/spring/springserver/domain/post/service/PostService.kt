package spring.springserver.domain.post.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse

interface PostService {

    fun createPost(
        createPostRequest: CreatePostRequest
    ): PostResponse

    fun viewPost(
        id: Long
    ): PostResponse

    fun viewAllPosts(
        pageable: Pageable
    ): Page<PostResponse>

    fun searchPostsByTitle(
        title: String,
        pageable: Pageable
    ): Page<PostResponse>

    fun updatePost(
        updatePostRequest: UpdatePostRequest
    ): PostResponse

    fun deletePost(
        id: Long
    ): DeletedPostResponse
}
