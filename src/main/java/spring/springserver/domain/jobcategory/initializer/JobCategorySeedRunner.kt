package spring.springserver.domain.jobcategory.initializer

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.jobcategory.entity.JobCategory
import spring.springserver.domain.jobcategory.repository.JobCategoryRepository

@Component
class JobCategorySeedRunner(
    private val jobCategoryRepository: JobCategoryRepository
): ApplicationRunner {

    companion object {

        private val log = LoggerFactory.getLogger(JobCategorySeedRunner::class.java)

        /**
         * 게시글 작성에 반드시 필요한 최상위 용역 카테고리.
         * 이 목록에 없는 카테고리도 업종으로 쓰이므로, 빠진 것만 채우고 지우지는 않는다.
         */
        private val DEFAULT_ROOT_CATEGORIES = listOf(
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

    @Transactional(rollbackFor = [Exception::class])
    override fun run(applicationArguments: ApplicationArguments) {

        val existingRootNames = jobCategoryRepository.findAllByParentIsNull()
            .map { category -> category.name }
            .toSet()

        val created = DEFAULT_ROOT_CATEGORIES.filterNot(existingRootNames::contains)
            .map {

                categoryName ->
                jobCategoryRepository.save(JobCategory(name = categoryName))
            }

        if (created.isNotEmpty()) {

            log.info("Seeded root job categories. (created: {})", created.size)
        }
    }
}
