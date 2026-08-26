package spring.springserver.domain.contract.service

import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.request.SignContractRequest
import spring.springserver.domain.contract.data.response.ContractResponse

interface ContractService {

    /**
     * 계약 당사자 중 한쪽이 갑·을과 계약 금액을 지정해 계약서 PDF를 등록한다.
     */
    fun createContract(
        createContractRequest: CreateContractRequest
    ): ContractResponse

    /**
     * 계약서 한 건의 현재 상태를 당사자에게 돌려준다.
     * PDF 자체는 채팅방에 첨부해 보여주므로 목록 조회는 두지 않는다.
     */
    fun getContract(
        contractId: Long
    ): ContractResponse

    /**
     * 로그인한 회원이 당사자로서 서명한다.
     * 갑과 을이 모두 서명하면 계약이 성립한다.
     */
    fun signContract(
        signContractRequest: SignContractRequest
    ): ContractResponse
}
