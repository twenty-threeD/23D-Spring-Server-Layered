package spring.springserver.domain.post.category.data.response

import spring.springserver.domain.post.category.entity.PostCategory

data class PostCategoryResponse(
    val id: Long?,

    val name: String,

    val parentId: Long?,

    val parentName: String?,

    val fullName: String
) {

    companion object {

        fun of(
            postCategory: PostCategory
        ): PostCategoryResponse {

            return PostCategoryResponse(
                postCategory.getId(),
                postCategory.name,
                postCategory.parent?.getId(),
                postCategory.parent?.name,
                postCategory.getFullName()
            )
        }
    }
}
