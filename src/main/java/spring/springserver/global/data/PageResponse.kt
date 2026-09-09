package spring.springserver.global.data

import org.springframework.data.domain.Page

/**
 * 페이지 응답 공통 형식.
 *
 * Spring Data의 Page를 그대로 직렬화하면 내부 구조가 응답 스펙이 되어
 * 라이브러리 버전에 따라 JSON이 바뀐다. 프론트가 읽는 필드만 골라 고정한다.
 */
data class PageResponse<T>(
    val content: List<T>,

    val page: Int,

    val size: Int,

    val totalElements: Long,

    val totalPages: Int,

    val last: Boolean,
) {

    companion object {

        fun <T> of(
            page: Page<*>,
            content: List<T>
        ): PageResponse<T> {

            return PageResponse(
                content,
                page.number,
                page.size,
                page.totalElements,
                page.totalPages,
                page.isLast,
            )
        }
    }
}
