package spring.springserver.domain.post.category.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.post.category.entity.PostCategory
import spring.springserver.domain.post.category.repository.PostCategoryRepository
import spring.springserver.domain.post.repository.PostRepository

@Component
class PostCategorySeedRunner(
    private val postCategoryRepository: PostCategoryRepository,
    private val postRepository: PostRepository
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(PostCategorySeedRunner::class.java)

        /**
         * 서비스에서 사용하는 용역 카테고리. 이 목록이 곧 카테고리 전체이므로,
         * 목록에 없는 카테고리는 시작할 때 정리되고 빠진 카테고리는 새로 추가된다.
         */
        private val DEFAULT_CATEGORIES = listOf(
            "이사/청소",
            "설치/수리",
            "인테리어",
            "외주",
            "법률/금융",
            "과외",
            "자동차",
            "기타"
        )
    }

    @Transactional
    override fun run(applicationArguments: ApplicationArguments) {

        val categories = postCategoryRepository.findAll()

        // 하위 카테고리는 더 이상 쓰지 않으므로, 최상위로 있으면서 목록에 있는 것만 그대로 둔다.
        val retained = categories.filter {

            category ->
            category.parent == null && DEFAULT_CATEGORIES.contains(category.name)
        }

        val obsolete = categories - retained.toSet()
        removeObsoleteCategories(obsolete)

        val retainedNames = retained.map { category -> category.name }.toSet()

        val created = DEFAULT_CATEGORIES.filterNot(retainedNames::contains)
            .map {

                categoryName ->
                postCategoryRepository.save(PostCategory(name = categoryName))
            }

        if (created.isNotEmpty() || obsolete.isNotEmpty()) {

            log.info("Synced post categories. (created: {}, removed: {})", created.size, obsolete.size)
        }
    }

    private fun removeObsoleteCategories(
        obsolete: List<PostCategory>
    ) {

        if (obsolete.isEmpty()) {

            return
        }

        val obsoleteIds = obsolete.mapNotNull(PostCategory::getId)

        // 사라지는 카테고리를 참조하던 게시글은 카테고리 없는 상태로 남긴다.
        postRepository.detachCategories(obsoleteIds)

        // 하위 카테고리가 상위 카테고리를 참조한 채로 지우면 FK 제약에 걸리므로 부모 참조부터 끊는다.
        postCategoryRepository.detachParents(obsoleteIds)

        postCategoryRepository.deleteAllById(obsoleteIds)
    }
}
