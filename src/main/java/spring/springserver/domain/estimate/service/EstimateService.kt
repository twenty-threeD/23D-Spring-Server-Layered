package spring.springserver.domain.estimate.service

import spring.springserver.domain.estimate.data.request.CreateEstimateRequest
import spring.springserver.domain.estimate.data.request.UpdateEstimateRequest
import spring.springserver.domain.estimate.data.response.DeleteEstimateResponse
import spring.springserver.domain.estimate.data.response.EstimateResponse

interface EstimateService {

    fun createEstimate(
        createEstimateRequest: CreateEstimateRequest
    ): EstimateResponse

    fun getEstimate(
        estimateId: Long
    ): EstimateResponse

    /**
     * 로그인한 회원이 의뢰인이거나 전문가인 견적서를 모두 반환한다.
     * postId를 넘기면 해당 게시글의 견적서로만 좁힌다.
     */
    fun getMyEstimates(
        postId: Long?
    ): List<EstimateResponse>

    fun updateEstimate(
        estimateId: Long,
        updateEstimateRequest: UpdateEstimateRequest
    ): EstimateResponse

    fun deleteEstimate(
        estimateId: Long
    ): DeleteEstimateResponse

    /**
     * 결제를 요청하기 전에 결제 도메인에서 호출한다.
     * 결제 대상 견적서인지, 금액이 최종 금액과 맞는지 확인한다.
     */
    fun validatePayable(
        estimateId: Long,
        memberId: Long,
        amount: Long
    )

    /**
     * 결제 승인 성공 시 결제 도메인에서 호출한다.
     * 견적서를 결제 완료 상태로 바꿔 이후 수정·삭제를 막는다.
     */
    fun markAsPaid(
        estimateId: Long,
        memberId: Long,
        paidAmount: Long
    ): EstimateResponse

    fun isProfessional(
        estimateId: Long,
        memberId: Long
    ): Boolean
}
