package spring.springserver.domain.post.service

interface PostRepeatStatsService {

    /**
     * 특정 글의 재구매 통계를 결제·계약을 다시 집계해 Post 에 반영한다.
     * 결제 confirm/cancel 처럼 집계 결과가 바뀌는 지점에서 호출한다.
     */
    fun recompute(
        postId: Long
    )

    /**
     * 계약 id 를 기준으로 재계산. 결제 측에서는 postId 를 모르고 contractId 만 알아 호출 편의용.
     */
    fun recomputeByContractId(
        contractId: Long?
    )

    /**
     * 전체 Post 에 대해 재계산을 돌린다. 백필·일괄 보정용.
     */
    fun recomputeAll(): Int
}
