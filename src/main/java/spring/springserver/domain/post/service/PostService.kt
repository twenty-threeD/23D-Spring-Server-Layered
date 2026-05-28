package spring.springserver.domain.post.service

import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse

interface PostService {

    fun createPost(createPostRequest: CreatePostRequest): PostResponse

    fun viewPost(id: Long): PostResponse

    fun updatePost(updatePostRequest: UpdatePostRequest): PostResponse

    fun deletePost(id: Long) : DeletedPostResponse
}
