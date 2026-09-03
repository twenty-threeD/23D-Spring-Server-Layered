package spring.springserver.domain.contract.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.response.CreateContractResponse
import spring.springserver.domain.contract.data.response.ViewContractResponse
import spring.springserver.domain.contract.service.ContractService
import spring.springserver.global.data.BaseResponse

@RestController
@RequestMapping("/api/contract")
class ContractController(
    private val contractService: ContractService
) {

    @PostMapping
    fun createContract(
        @Valid @RequestBody createContractRequest: CreateContractRequest
    ): BaseResponse<CreateContractResponse> {

        return BaseResponse.ok(contractService.createContract(createContractRequest))
    }

    @GetMapping("/{contractId}")
    fun getContract(
        @PathVariable contractId: Long,
        httpServletRequest: HttpServletRequest
    ): BaseResponse<ViewContractResponse> {

        return BaseResponse.ok(contractService.getContract(
            contractId,
            httpServletRequest
        ))
    }
}
