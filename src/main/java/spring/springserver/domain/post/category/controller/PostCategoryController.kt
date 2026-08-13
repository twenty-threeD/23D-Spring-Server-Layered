package spring.springserver.domain.post.category.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.post.category.data.response.PostCategoryResponse
import spring.springserver.domain.post.category.service.PostCategoryService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post-category")
class PostCategoryController(
    private val postCategoryService: PostCategoryService
) {

    @GetMapping
    fun getPostCategories(): BaseResponse<List<PostCategoryResponse>> {

        return BaseResponse.ok(postCategoryService.getPostCategories())
    }
}
