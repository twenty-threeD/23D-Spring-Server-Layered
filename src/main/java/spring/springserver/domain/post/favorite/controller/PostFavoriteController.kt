package spring.springserver.domain.post.favorite.controller

import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.favorite.data.response.PostFavoriteResponse
import spring.springserver.domain.post.favorite.service.PostFavoriteService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/post/favorite")
class PostFavoriteController(
    private val postFavoriteService: PostFavoriteService
) {

    @PostMapping
    fun favoritePost(
        @RequestParam postId: Long
    ): BaseResponse<PostFavoriteResponse> {

        return BaseResponse.ok(postFavoriteService.favoritePost(postId))
    }

    @DeleteMapping
    fun unfavoritePost(
        @RequestParam postId: Long
    ): BaseResponse<PostFavoriteResponse> {

        return BaseResponse.ok(postFavoriteService.unfavoritePost(postId))
    }

    @GetMapping
    fun viewFavoritePosts(
        @ParameterObject pageable: Pageable
    ): BaseResponse<Page<PostResponse>> {

        return BaseResponse.ok(postFavoriteService.viewFavoritePosts(pageable))
    }
}
