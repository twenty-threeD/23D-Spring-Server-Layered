package spring.springserver.domain.post.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.service.PostService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    fun createPost(
        @Valid @RequestBody createPostRequest: CreatePostRequest
    ): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.createPost(createPostRequest))
    }

    @GetMapping
    fun viewPost(
        @RequestParam postId: Long
    ): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.viewPost(postId))
    }

    @GetMapping("/all")
    fun viewAllPosts(): BaseResponse<List<PostResponse>> {

        return BaseResponse.ok(postService.viewAllPosts())
    }

    @GetMapping("/search")
    fun searchPostsByTitle(
        @RequestParam title: String
    ): BaseResponse<List<PostResponse>> {

        return BaseResponse.ok(postService.searchPostsByTitle(title))
    }

    @PatchMapping
    fun updatePost(
        @Valid @RequestBody updatePostRequest: UpdatePostRequest
    ): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.updatePost(updatePostRequest))
    }

    @DeleteMapping
    fun deletePost(
        @RequestParam postId: Long
    ): BaseResponse<DeletedPostResponse> {

        return BaseResponse.ok(postService.deletePost(postId))
    }
}
