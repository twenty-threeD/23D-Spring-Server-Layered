package spring.springserver.domain.contract.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.request.SignContractRequest
import spring.springserver.domain.contract.data.request.UpdateContractRequest
import spring.springserver.domain.contract.data.response.ContractResponse
import spring.springserver.domain.contract.data.response.DeleteContractResponse
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
    ): BaseResponse<ContractResponse> {

        return BaseResponse.ok(contractService.createContract(createContractRequest))
    }

    @GetMapping("/{contractId}")
    fun getContract(
        @PathVariable contractId: Long
    ): BaseResponse<ContractResponse> {

        return BaseResponse.ok(contractService.getContract(contractId))
    }

    @GetMapping
    fun getMyContracts(): BaseResponse<List<ContractResponse>> {

        return BaseResponse.ok(contractService.getMyContracts())
    }

    @PatchMapping
    fun updateContract(
        @Valid @RequestBody updateContractRequest: UpdateContractRequest
    ): BaseResponse<ContractResponse> {

        return BaseResponse.ok(contractService.updateContract(updateContractRequest))
    }

    @PostMapping("/sign")
    fun signContract(
        @Valid @RequestBody signContractRequest: SignContractRequest
    ): BaseResponse<ContractResponse> {

        return BaseResponse.ok(contractService.signContract(signContractRequest))
    }

    @DeleteMapping
    fun deleteContract(
        @RequestParam contractId: Long
    ): BaseResponse<DeleteContractResponse> {

        return BaseResponse.ok(contractService.deleteContract(contractId))
    }
}
