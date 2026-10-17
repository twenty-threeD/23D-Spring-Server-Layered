package spring.springserver.global.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class PageableSortResolverTest {

    private val allowed = setOf("updatedAt", "viewCount", "id")

    private val default = Sort.by(Sort.Direction.DESC, "updatedAt")

    private val tiebreaker = Sort.by(Sort.Direction.DESC, "id")

    @Test
    fun `정렬이 비어 있으면 기본 정렬 + tiebreaker 를 돌려준다`() {

        val pageable = PageRequest.of(
            0,
            10
        )

        val resolved = PageableSortResolver.resolve(
            pageable,
            allowed,
            default,
            tiebreaker
        )

        assertThat(resolved.sort.toList())
            .extracting<String> { order -> "${order.property},${order.direction}" }
            .containsExactly(
                "updatedAt,DESC",
                "id,DESC"
            )
    }

    @Test
    fun `허용 필드 정렬이 들어오면 그대로 쓰고 tiebreaker 를 뒤에 붙인다`() {

        val pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Direction.DESC, "viewCount")
        )

        val resolved = PageableSortResolver.resolve(
            pageable,
            allowed,
            default,
            tiebreaker
        )

        assertThat(resolved.sort.toList())
            .extracting<String> { order -> "${order.property},${order.direction}" }
            .containsExactly(
                "viewCount,DESC",
                "id,DESC"
            )
    }

    @Test
    fun `허용 밖 필드만 오면 기본 정렬로 대체한다`() {

        val pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Direction.ASC, "foo")
        )

        val resolved = PageableSortResolver.resolve(
            pageable,
            allowed,
            default,
            tiebreaker
        )

        assertThat(resolved.sort.toList())
            .extracting<String> { order -> "${order.property},${order.direction}" }
            .containsExactly(
                "updatedAt,DESC",
                "id,DESC"
            )
    }

    @Test
    fun `허용 밖 필드는 걸러내고 허용 필드만 남긴다`() {

        val pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Order.desc("viewCount"), Sort.Order.asc("foo"))
        )

        val resolved = PageableSortResolver.resolve(
            pageable,
            allowed,
            default,
            tiebreaker
        )

        assertThat(resolved.sort.toList())
            .extracting<String> { order -> "${order.property},${order.direction}" }
            .containsExactly(
                "viewCount,DESC",
                "id,DESC"
            )
    }

    @Test
    fun `page 번호와 size 는 그대로 유지한다`() {

        val pageable = PageRequest.of(
            3,
            25,
            Sort.by(Sort.Direction.ASC, "updatedAt")
        )

        val resolved = PageableSortResolver.resolve(
            pageable,
            allowed,
            default,
            tiebreaker
        )

        assertThat(resolved.pageNumber).isEqualTo(3)
        assertThat(resolved.pageSize).isEqualTo(25)
    }
}
