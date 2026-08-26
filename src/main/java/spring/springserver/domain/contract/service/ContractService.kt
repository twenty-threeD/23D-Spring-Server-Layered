package spring.springserver.domain.contract.service

import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.request.SignContractRequest
import spring.springserver.domain.contract.data.request.UpdateContractRequest
import spring.springserver.domain.contract.data.response.ContractResponse
import spring.springserver.domain.contract.data.response.DeleteContractResponse

interface ContractService {

    /**
     * 계약 당사자 중 한쪽이 갑·을과 계약 금액을 지정해 계약서 PDF를 등록한다.
     */
    fun createContract(
        createContractRequest: CreateContractRequest
    ): ContractResponse

    fun getContract(
        contractId: Long
    ): ContractResponse

    /**
     * 로그인한 회원이 갑이거나 을인 계약서를 모두 반환한다.
     */
    fun getMyContracts(): List<ContractResponse>

    /**
     * 아직 아무도 서명하지 않은 계약서만 등록자가 고칠 수 있다.
     */
    fun updateContract(
        updateContractRequest: UpdateContractRequest
    ): ContractResponse

    /**
     * 로그인한 회원이 당사자로서 서명한다.
     * 갑과 을이 모두 서명하면 계약이 성립한다.
     */
    fun signContract(
        signContractRequest: SignContractRequest
    ): ContractResponse

    fun deleteContract(
        contractId: Long
    ): DeleteContractResponse
}
