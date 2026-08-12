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

    fun getMyEstimates(): List<EstimateResponse>

    fun updateEstimate(
        estimateId: Long,
        updateEstimateRequest: UpdateEstimateRequest
    ): EstimateResponse

    fun deleteEstimate(
        estimateId: Long
    ): DeleteEstimateResponse

    /**
     * 결제 승인 성공 시 결제 도메인에서 호출한다.
     * 견적서를 결제 완료 상태로 바꿔 이후 수정·삭제를 막는다.
     */
    fun markAsPaid(
        estimateId: Long,
        memberId: Long
    ): EstimateResponse
}
