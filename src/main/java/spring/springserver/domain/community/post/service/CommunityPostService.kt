package spring.springserver.domain.community.post.service

import spring.springserver.domain.community.common.data.response.DeleteResponse
import spring.springserver.domain.community.post.data.request.CreatePostRequest
import spring.springserver.domain.community.post.data.request.UpdatePostRequest
import spring.springserver.domain.community.post.data.response.CommunityPostResponse
import spring.springserver.domain.community.post.data.response.CreatePostResponse
import spring.springserver.domain.community.post.data.response.UpdatePostResponse
import org.springframework.web.multipart.MultipartFile

interface CommunityPostService {

    fun createPost(
        createPostRequest: CreatePostRequest,
        file: MultipartFile? = null
    ): CreatePostResponse

    fun createPost(
        createPostRequestJson: String,
        file: MultipartFile?
    ): CreatePostResponse

    fun updatePost(
        updatePostRequest: UpdatePostRequest,
        file: MultipartFile? = null
    ): UpdatePostResponse

    fun updatePost(
        updatePostRequestJson: String,
        file: MultipartFile?
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