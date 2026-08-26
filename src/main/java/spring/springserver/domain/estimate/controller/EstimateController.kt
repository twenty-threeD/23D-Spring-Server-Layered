package spring.springserver.domain.estimate.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.estimate.data.request.CreateEstimateRequest
import spring.springserver.domain.estimate.data.request.UpdateEstimateRequest
import spring.springserver.domain.estimate.data.response.DeleteEstimateResponse
import spring.springserver.domain.estimate.data.response.EstimateResponse
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/estimate")
class EstimateController(
    private val estimateService: EstimateService
) {

    @PostMapping
    fun createEstimate(
        @Valid @RequestBody createEstimateRequest: CreateEstimateRequest
    ): BaseResponse<EstimateResponse> {

        return BaseResponse.ok(estimateService.createEstimate(createEstimateRequest))
    }

    @GetMapping("/{estimateId}")
    fun getEstimate(
        @PathVariable estimateId: Long
    ): BaseResponse<EstimateResponse> {

        return BaseResponse.ok(estimateService.getEstimate(estimateId))
    }

    @GetMapping
    fun getMyEstimates(
        @RequestParam(required = false) postId: Long?
    ): BaseResponse<List<EstimateResponse>> {

        return BaseResponse.ok(estimateService.getMyEstimates(postId))
    }

    @PatchMapping("/{estimateId}")
    fun updateEstimate(
        @PathVariable estimateId: Long,
        @Valid @RequestBody updateEstimateRequest: UpdateEstimateRequest
    ): BaseResponse<EstimateResponse> {

        return BaseResponse.ok(
            estimateService.updateEstimate(
                estimateId,
                updateEstimateRequest
            )
        )
    }

    @DeleteMapping("/{estimateId}")
    fun deleteEstimate(
        @PathVariable estimateId: Long
    ): BaseResponse<DeleteEstimateResponse> {

        return BaseResponse.ok(estimateService.deleteEstimate(estimateId))
    }
}
