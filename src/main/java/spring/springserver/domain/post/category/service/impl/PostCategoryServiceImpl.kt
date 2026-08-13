package spring.springserver.domain.post.category.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.post.category.data.response.PostCategoryResponse
import spring.springserver.domain.post.category.entity.PostCategory
import spring.springserver.domain.post.category.exception.PostCategoryStatusCode
import spring.springserver.domain.post.category.repository.PostCategoryRepository
import spring.springserver.domain.post.category.service.PostCategoryService
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(readOnly = true)
class PostCategoryServiceImpl(
    private val postCategoryRepository: PostCategoryRepository
): PostCategoryService {

    override fun getPostCategories(): List<PostCategoryResponse> {

        return postCategoryRepository.findAllByOrderByNameAsc()
            .map {
                postCategory -> PostCategoryResponse.of(postCategory)
            }
    }

    override fun getPostCategory(
        postCategoryId: Long
    ): PostCategory {

        return postCategoryRepository.findById(postCategoryId).orElse(null)
            ?: throw ApplicationException(PostCategoryStatusCode.POST_CATEGORY_NOT_FOUND)
    }

    override fun getCategoryIdsIncludingDescendants(
        postCategoryId: Long
    ): List<Long> {

        val rootId = getPostCategory(postCategoryId).getId()!!

        val collected = linkedSetOf(rootId)
        var currentLevel = listOf(rootId)

        while (currentLevel.isNotEmpty()) {

            // 이미 담긴 id는 걸러내므로, 데이터가 순환하더라도 무한 루프가 되지 않는다.
            currentLevel = postCategoryRepository.findIdsByParentIdIn(currentLevel)
                .filter { childId -> collected.add(childId) }
        }

        return collected.toList()
    }
}
