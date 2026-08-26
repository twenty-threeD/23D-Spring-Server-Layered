package spring.springserver.domain.contract.service.impl

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.request.SignContractRequest
import spring.springserver.domain.contract.data.request.UpdateContractRequest
import spring.springserver.domain.contract.data.response.ContractResponse
import spring.springserver.domain.contract.data.response.DeleteContractResponse
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.contract.exception.ContractStatusCode
import spring.springserver.domain.contract.repository.ContractRepository
import spring.springserver.domain.contract.service.ContractService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class ContractServiceImpl(
    private val contractRepository: ContractRepository,
    private val memberRepository: MemberRepository
): ContractService {

    override fun createContract(
        createContractRequest: CreateContractRequest
    ): ContractResponse {

        /**
         * 계약서를 올리는 사람은 반드시 그 계약의 당사자여야 한다.
         * 제3자가 남의 계약서를 만들어 두면 서명 주체를 신뢰할 수 없게 된다.
         */
        val writer = getCurrentMember()

        val partA = getMemberEntity(createContractRequest.partA!!)
        val partB = getMemberEntity(createContractRequest.partB!!)

        if (partA.getId() == partB.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_INVALID_MEMBER)
        }

        if (writer.getId() != partA.getId() && writer.getId() != partB.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }

        val contract = contractRepository.save(
            Contract(
                partA = partA,
                partB = partB,
                writer = writer,
                contractUrl = normalizeUrl(createContractRequest.contractUrl!!),
                amount = createContractRequest.amount!!
            )
        )

        return ContractResponse.of(contract)
    }

    @Transactional(readOnly = true)
    override fun getContract(
        contractId: Long
    ): ContractResponse {

        val member = getCurrentMember()
        val contract = getContractEntity(contractId)

        validateParticipant(contract, member)

        return ContractResponse.of(contract)
    }

    @Transactional(readOnly = true)
    override fun getMyContracts(): List<ContractResponse> {

        val contracts = contractRepository.findAllByParticipant(getCurrentMember())

        return contracts.map { ContractResponse.of(it) }
    }

    override fun updateContract(
        updateContractRequest: UpdateContractRequest
    ): ContractResponse {

        val member = getCurrentMember()
        val contract = getContractEntity(updateContractRequest.contractId!!)

        validateWriter(contract, member)
        validateNotSigned(contract)

        contract.update(
            normalizeUrl(updateContractRequest.contractUrl!!),
            updateContractRequest.amount!!
        )

        return ContractResponse.of(contract)
    }

    override fun signContract(
        signContractRequest: SignContractRequest
    ): ContractResponse {

        val member = getCurrentMember()
        val contract = getContractEntity(signContractRequest.contractId!!)

        if (contract.partA.getId() == member.getId()) {

            if (contract.isPartASigned()) {

                throw ApplicationException(ContractStatusCode.CONTRACT_ALREADY_SIGNED_BY_MEMBER)
            }

            contract.signAsPartA()
        } else if (contract.partB.getId() == member.getId()) {

            if (contract.isPartBSigned()) {

                throw ApplicationException(ContractStatusCode.CONTRACT_ALREADY_SIGNED_BY_MEMBER)
            }

            contract.signAsPartB()
        } else {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }

        return ContractResponse.of(contract)
    }

    override fun deleteContract(
        contractId: Long
    ): DeleteContractResponse {

        val member = getCurrentMember()
        val contract = getContractEntity(contractId)

        validateWriter(contract, member)
        validateNotSigned(contract)

        contractRepository.delete(contract)

        return DeleteContractResponse.of("계약서가 삭제되었습니다.")
    }

    private fun getContractEntity(
        contractId: Long
    ): Contract {

        return contractRepository.findById(contractId).orElseThrow {

            ApplicationException(ContractStatusCode.CONTRACT_NOT_FOUND)
        }
    }

    private fun getMemberEntity(
        memberId: Long
    ): Member {

        return memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
    }

    /**
     * 계약서는 PDF로만 주고받기로 했으므로 업로드된 PDF 경로인지 확인한다.
     */
    private fun normalizeUrl(
        contractUrl: String
    ): String {

        val normalizedUrl = contractUrl.trim()

        if (!normalizedUrl.lowercase().endsWith(PDF_EXTENSION)) {

            throw ApplicationException(ContractStatusCode.CONTRACT_INVALID_FILE)
        }

        return normalizedUrl
    }

    /**
     * 한쪽이 서명한 뒤에 내용이 바뀌면 그 서명이 무엇에 대한 동의였는지 알 수 없게 된다.
     * 그래서 서명이 하나라도 있으면 수정·삭제를 막는다.
     */
    private fun validateNotSigned(
        contract: Contract
    ) {

        if (contract.hasAnySignature()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_ALREADY_SIGNED)
        }
    }

    /**
     * 계약서를 올린 당사자만 계약서를 교체하거나 거둘 수 있다.
     */
    private fun validateWriter(
        contract: Contract,
        member: Member
    ) {

        if (contract.writer.getId() != member.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }
    }

    private fun validateParticipant(
        contract: Contract,
        member: Member
    ) {

        if (contract.partA.getId() != member.getId() &&
            contract.partB.getId() != member.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }
    }

    private fun getCurrentMember(): Member {

        val username = SecurityContextHolder.getContext().authentication?.name

        if (username.isNullOrBlank() || username == "anonymousUser") {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
    }

    companion object {

        private const val PDF_EXTENSION = ".pdf"
    }
}
