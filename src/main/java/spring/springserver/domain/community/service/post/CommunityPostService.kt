package spring.springserver.domain.community.service.post

import spring.springserver.domain.community.data.request.CreatePostRequest
import spring.springserver.domain.community.data.request.UpdatePostRequest
import spring.springserver.domain.community.data.response.CommunityPostResponse
import spring.springserver.domain.community.data.response.CreatePostResponse
import spring.springserver.domain.community.data.response.UpdatePostResponse

interface CommunityPostService {

    fun createPost(createPostRequest: CreatePostRequest): CreatePostResponse

    fun updatePost(updatePostRequest: UpdatePostRequest): UpdatePostResponse

    fun deletePost(postId: Long)

    fun getPost(postId: Long): CommunityPostResponse

    fun searchPosts(keyword: String): List<CommunityPostResponse>
}
