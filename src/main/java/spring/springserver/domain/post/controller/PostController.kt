package spring.springserver.domain.post.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.post.dto.request.CreatePostRequest
import spring.springserver.domain.post.dto.request.UpdatePostRequest
import spring.springserver.domain.post.dto.response.PostResponse
import spring.springserver.domain.post.service.PostService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post")
class PostController(private val postService: PostService) {

    @PostMapping("/create")
    fun createPost(@Valid @RequestBody createPostRequest: CreatePostRequest): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.createPost(createPostRequest))
    }

    @GetMapping("/findById")
    fun findPostById(@RequestBody id: Long): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.findPost(id))
    }

    @PatchMapping("/update")
    fun updatePost(@Valid @RequestBody id: Long,
                   @Valid @RequestBody updatePostRequest: UpdatePostRequest): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.updatePost(
            id,
            updatePostRequest
        )
        )
    }

    @DeleteMapping("/delete")
    fun deletePost(@RequestBody id: Long): BaseResponse<PostResponse> {

        return BaseResponse.ok(postService.deletePost(id))
    }
}
