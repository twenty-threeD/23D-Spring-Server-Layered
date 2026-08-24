package spring.springserver.domain.estimate.entity

/**
 * 견적서의 결제 진행 상태.
 * 전문가가 제안한 시점에는 PROPOSED이고, 의뢰인이 수락해 결제를 마치면 PAID가 된다.
 */
enum class EstimateStatus {

    PROPOSED,
    PAID
}
