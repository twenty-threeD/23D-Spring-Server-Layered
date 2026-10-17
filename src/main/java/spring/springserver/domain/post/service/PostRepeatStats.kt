package spring.springserver.domain.post.service

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 재구매율 집계 값. 입력은 "같은 글을 결제 완료한 서로 다른 갑마다의 결제 건수" 리스트다.
 */
data class PostRepeatStats(
    val buyerCount: Int,
    val repeatBuyerCount: Int,
    val repeatRate: Double
) {

    companion object {

        private const val REPEAT_THRESHOLD = 2L

        fun of(
            perBuyerCounts: List<Long>
        ): PostRepeatStats {

            val buyerCount = perBuyerCounts.size
            val repeatBuyerCount = perBuyerCounts.count { count -> count >= REPEAT_THRESHOLD }
            val repeatRate = if (buyerCount == 0) {

                0.0
            } else {

                BigDecimal(repeatBuyerCount)
                    .divide(BigDecimal(buyerCount), 2, RoundingMode.HALF_UP)
                    .toDouble()
            }

            return PostRepeatStats(
                buyerCount,
                repeatBuyerCount,
                repeatRate
            )
        }
    }
}
