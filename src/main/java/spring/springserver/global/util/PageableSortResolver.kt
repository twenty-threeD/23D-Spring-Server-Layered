package spring.springserver.global.util

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * 외부에서 들어온 Pageable 의 정렬을 안전한 범위로 제한한다.
 * JPA Repository 에 엔티티에 없는 필드가 넘어가면 PropertyReferenceException 으로 500 이 난다.
 * 허용 목록에 없는 필드는 버리고, 전부 걸러져 비면 default 를 쓰며,
 * 마지막에 안정 정렬용 tiebreaker 를 항상 붙여 같은 값끼리 페이지 넘김이 흔들리지 않게 한다.
 */
object PageableSortResolver {

    fun resolve(
        pageable: Pageable,
        allowed: Set<String>,
        default: Sort,
        tiebreaker: Sort
    ): Pageable {

        val filtered = pageable.sort
            .toList()
            .filter { order -> allowed.contains(order.property) }

        val base = if (filtered.isEmpty()) {

            default
        } else {

            Sort.by(filtered)
        }

        return PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            base.and(tiebreaker)
        )
    }
}
