package spring.springserver.domain.community.post.service

import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse

interface CommunityPostService {

    fun createPost(
        createPostRequest: CreatePostRequest
    ): CreatePostResponse

    fun updatePost(
        updatePostRequest: UpdatePostRequest
    ): UpdatePostResponse

    fun deletePost(
        postId: Long
    ): DeleteResponse

    fun getPosts(): List<CommunityPostResponse>

    fun getPost(
        postId: Long
    ): CommunityPostResponse

    fun searchPosts(
        keyword: String
    ): List<CommunityPostResponse>
}
