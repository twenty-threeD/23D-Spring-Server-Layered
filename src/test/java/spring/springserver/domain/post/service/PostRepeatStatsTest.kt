package spring.springserver.domain.post.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostRepeatStatsTest {

    @Test
    fun `구매자 3명 중 1명이 재구매면 재구매율은 소수점 둘째 자리 반올림한 0_33`() {

        val stats = PostRepeatStats.of(listOf(2L, 1L, 1L))

        assertThat(stats.buyerCount).isEqualTo(3)
        assertThat(stats.repeatBuyerCount).isEqualTo(1)
        assertThat(stats.repeatRate).isEqualTo(0.33)
    }

    @Test
    fun `구매자 3명 모두 재구매면 재구매율은 1_0`() {

        val stats = PostRepeatStats.of(listOf(3L, 2L, 2L))

        assertThat(stats.buyerCount).isEqualTo(3)
        assertThat(stats.repeatBuyerCount).isEqualTo(3)
        assertThat(stats.repeatRate).isEqualTo(1.0)
    }

    @Test
    fun `결제가 하나도 없으면 모든 값이 0`() {

        val stats = PostRepeatStats.of(emptyList())

        assertThat(stats.buyerCount).isEqualTo(0)
        assertThat(stats.repeatBuyerCount).isEqualTo(0)
        assertThat(stats.repeatRate).isEqualTo(0.0)
    }

    @Test
    fun `구매자 1명이 2번 사도 buyerCount 는 1 이고 repeatBuyerCount 는 1`() {

        val stats = PostRepeatStats.of(listOf(2L))

        assertThat(stats.buyerCount).isEqualTo(1)
        assertThat(stats.repeatBuyerCount).isEqualTo(1)
        assertThat(stats.repeatRate).isEqualTo(1.0)
    }

    @Test
    fun `취소로 완료 결제가 1건으로 줄면 재구매가 사라진다`() {

        val afterCancel = PostRepeatStats.of(listOf(1L, 1L, 1L))

        assertThat(afterCancel.buyerCount).isEqualTo(3)
        assertThat(afterCancel.repeatBuyerCount).isEqualTo(0)
        assertThat(afterCancel.repeatRate).isEqualTo(0.0)
    }
}
