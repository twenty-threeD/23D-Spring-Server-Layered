package spring.springserver.domain.post.favorite.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import spring.springserver.domain.post.data.response.PostResponse
import spring.springserver.domain.post.favorite.data.response.PostFavoriteResponse

interface PostFavoriteService {

    fun favoritePost(
        postId: Long
    ): PostFavoriteResponse

    fun unfavoritePost(
        postId: Long
    ): PostFavoriteResponse

    fun viewFavoritePosts(
        pageable: Pageable
    ): Page<PostResponse>
}
