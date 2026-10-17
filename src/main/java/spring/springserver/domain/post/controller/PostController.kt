package spring.springserver.domain.post.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.service.PostService
import spring.springserver.global.data.BaseResponse
import spring.springserver.global.util.ClientIpResolver

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

    @GetMapping ("/{postId}")
    fun viewPost(
        @PathVariable postId: Long,
        httpServletRequest: HttpServletRequest
    ): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.viewPost(
                postId,
                ClientIpResolver.resolve(httpServletRequest),
                httpServletRequest.getHeader(HttpHeaders.USER_AGENT).orEmpty()
        ))
    }

    @GetMapping
    fun viewAllPosts(
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostResponse>> {

        return BaseResponse.ok(postService.viewAllPosts(pageable))
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) categoryId: Long?,
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostResponse>> {

        return BaseResponse.ok(postService.searchPosts(title, categoryId, pageable))
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
