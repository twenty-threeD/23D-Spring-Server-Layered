package spring.springserver.domain.payment.policy

/**
 * 결제 금액 구간별 플랫폼 수수료 정책.
 *
 * 의뢰인은 계약 금액 그대로 결제하고, 수수료는 그 금액에서 떼어 전문가 정산액이 줄어든다.
 * 원 단위 미만은 올림한다.
 *
 * - 10만 원 이하: 5%
 * - 10만 원 초과 ~ 100만 원 이하: 4%
 * - 100만 원 초과 ~ 500만 원 이하: 3%
 * - 500만 원 초과 ~ 1,000만 원 미만: 2%
 * - 1,000만 원 이상: 1%
 */
object PaymentFeePolicy {

    private const val HUNDRED_THOUSAND = 100_000L
    private const val ONE_MILLION = 1_000_000L
    private const val FIVE_MILLION = 5_000_000L
    private const val TEN_MILLION = 10_000_000L

    fun feeRatePercent(
        amount: Long
    ): Long {

        return when {

            amount <= HUNDRED_THOUSAND -> 5L
            amount <= ONE_MILLION -> 4L
            amount <= FIVE_MILLION -> 3L
            amount < TEN_MILLION -> 2L
            else -> 1L
        }
    }

    /**
     * 실수 연산의 오차를 피하려고 정수로 계산한다. (금액 × 비율 + 99) / 100 이 올림이다.
     */
    fun calculateFee(
        amount: Long
    ): Long {

        return (amount * feeRatePercent(amount) + 99) / 100
    }

    fun calculateSettlement(
        amount: Long
    ): Long {

        return amount - calculateFee(amount)
    }
}