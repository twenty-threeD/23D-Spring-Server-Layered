package spring.springserver.domain.post.controller

import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import spring.springserver.domain.post.data.request.CreatePostRequest
import spring.springserver.domain.post.data.request.UpdatePostRequest
import spring.springserver.domain.post.data.response.DeletedPostResponse
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.service.PostService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post")
class PostController(private val postService: PostService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPost(
        @Valid @RequestPart("request") createPostRequest: CreatePostRequest,
        @RequestPart("multipartFile", required = false) multipartFile: MultipartFile?
    ): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.createPost(createPostRequest, multipartFile))
    }

    @GetMapping
    fun viewPost(@RequestParam postId: Long): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.viewPost(postId))
    }

    @PatchMapping
    fun updatePost(@Valid @RequestBody updatePostRequest: UpdatePostRequest): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.updatePost(updatePostRequest))
    }

    @DeleteMapping
    fun deletePost(@RequestParam postId: Long): BaseResponse<DeletedPostResponse> {

        return BaseResponse.ok(postService.deletePost(postId))
    }
}
