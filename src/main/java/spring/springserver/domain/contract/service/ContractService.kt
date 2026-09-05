package spring.springserver.domain.contract.service

import jakarta.servlet.http.HttpServletRequest
import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.response.CreateContractResponse
import spring.springserver.domain.contract.data.response.ViewContractResponse

interface ContractService {

    /**
     * 계약 당사자 중 한쪽이 의뢰인(갑)·전문가(을)과 계약 금액을 지정해 계약서 PDF를 등록한다.
     */
    fun createContract(
        createContractRequest: CreateContractRequest
    ): CreateContractResponse

    /**
     * 계약서 한 건의 현재 상태를 당사자에게 돌려준다.
     * PDF 자체는 채팅방에 첨부해 보여주므로 목록 조회는 두지 않는다.
     */
    fun getContract(
        contractId: Long,
        httpServletRequest: HttpServletRequest
    ): ViewContractResponse

    /**
     * 계약서 PDF 경로로 계약 당사자 여부를 확인한다.
     * 결제 건은 계약서 URL만 들고 있어 온체인 검증에서 판매자를 가려낼 때 호출한다.
     * 계약서를 찾지 못하면 당사자가 아닌 것으로 본다.
     */
    fun isParty(
        contractUrl: String,
        memberId: Long
    ): Boolean
}
