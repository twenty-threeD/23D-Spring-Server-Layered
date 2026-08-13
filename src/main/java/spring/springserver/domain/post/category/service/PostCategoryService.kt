package spring.springserver.domain.post.category.service

import spring.springserver.domain.post.category.data.response.PostCategoryResponse
import spring.springserver.domain.post.category.entity.PostCategory

interface PostCategoryService {

    fun getPostCategories(): List<PostCategoryResponse>

    fun getPostCategory(
        postCategoryId: Long
    ): PostCategory

    /**
     * 주어진 카테고리와 그 하위 카테고리 전체의 id를 반환한다.
     * 상위 카테고리로 검색했을 때 하위 카테고리 게시글까지 조회하기 위해 쓰인다.
     */
    fun getCategoryIdsIncludingDescendants(
        postCategoryId: Long
    ): List<Long>
}
