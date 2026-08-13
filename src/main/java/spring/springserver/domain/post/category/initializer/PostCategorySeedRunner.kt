package spring.springserver.domain.post.category.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.post.category.entity.PostCategory
import spring.springserver.domain.post.category.repository.PostCategoryRepository

@Component
class PostCategorySeedRunner(
    private val postCategoryRepository: PostCategoryRepository
) : ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(PostCategorySeedRunner::class.java)

        /**
         * 초기 용역 카테고리. 테이블이 비어 있을 때만 넣으므로 운영 데이터를 덮어쓰지 않는다.
         * 실제 서비스 분류에 맞게 자유롭게 수정하면 된다.
         */
        private val DEFAULT_CATEGORIES = linkedMapOf(
            "건설·시공" to listOf("전기", "배관", "목공", "도배·장판", "타일", "미장·방수", "철거"),
            "인테리어" to listOf("주방", "욕실", "샷시·창호", "조명"),
            "청소" to listOf("입주청소", "사무실청소", "에어컨청소"),
            "설치·수리" to listOf("가전 설치", "가구 조립", "보일러 수리"),
            "이사·운반" to listOf("가정 이사", "사무실 이사", "용달")
        )
    }

    @Transactional
    override fun run(args: ApplicationArguments) {

        if (postCategoryRepository.count() > 0L) {

            return
        }

        var createdCount = 0

        DEFAULT_CATEGORIES.forEach {

            (parentName, childNames) ->
            val parent = postCategoryRepository.save(PostCategory(name = parentName))
            createdCount++

            childNames.forEach {

                childName ->
                postCategoryRepository.save(
                    PostCategory(
                        name = childName,
                        parent = parent
                    )
                )
                createdCount++
            }
        }

        log.info("Seeded {} post category(s).", createdCount)
    }
}
