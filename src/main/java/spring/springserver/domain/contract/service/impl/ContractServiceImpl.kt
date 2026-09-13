package spring.springserver.domain.contract.service.impl

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.auth.service.token.TokenService
import spring.springserver.domain.contract.data.request.CreateContractRequest
import spring.springserver.domain.contract.data.response.ContractPartyResponse
import spring.springserver.domain.contract.data.response.CreateContractResponse
import spring.springserver.domain.contract.data.response.ViewContractResponse
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.contract.exception.ContractStatusCode
import spring.springserver.domain.contract.repository.ContractRepository
import spring.springserver.domain.contract.service.ContractService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException
import java.time.LocalDateTime

@Service
@Transactional(rollbackFor = [Exception::class])
class ContractServiceImpl(
    private val contractRepository: ContractRepository,
    private val memberRepository: MemberRepository,
    private val tokenService: TokenService
): ContractService {

    override fun createContract(
        createContractRequest: CreateContractRequest
    ): CreateContractResponse {

        val writer = getCurrentMember()

        val client = getMemberEntity(createContractRequest.clientId!!)
        val professional = getMemberEntity(createContractRequest.professionalId!!)

        if (client.getId() == professional.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_INVALID_MEMBER)
        }

        if (writer.getId() != client.getId() && writer.getId() != professional.getId()) {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }

        validatePeriod(
            startedAt = createContractRequest.startedAt,
            endedAt = createContractRequest.endedAt
        )

        val contract = contractRepository.save(
            Contract(
                client = client,
                professional = professional,
                writer = writer,
                startedAt = createContractRequest.startedAt,
                endedAt = createContractRequest.endedAt,
                inspectionPeriod = createContractRequest.inspectionPeriod!!,
                price = createContractRequest.price!!,
                servicesDescription = createContractRequest.servicesDescription!!.trim()
            )
        )

        return CreateContractResponse.of(contract)
    }

    @Transactional(readOnly = true)
    override fun getContract(
        contractId: Long,
        httpServletRequest: HttpServletRequest
    ): ViewContractResponse {

        val contract = getContractEntity(contractId)

        validateParticipant(
            contract,
            memberRepository.findByUsername(tokenService.getCurrentUsername(httpServletRequest))
                ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
        )

        return ViewContractResponse.of(contract)
    }

    @Transactional(readOnly = true)
    override fun findPartyById(
        contractId: Long
    ): ContractPartyResponse? {

        val contract = contractRepository.findContractById(contractId)
            ?: return null

        return ContractPartyResponse.of(contract = contract)
    }

    private fun getContractEntity(
        contractId: Long
    ): Contract {

        return contractRepository.findById(contractId).orElseThrow { ApplicationException(ContractStatusCode.CONTRACT_NOT_FOUND) }
    }

    /**
     * 계약서는 의뢰인(갑)·전문가(을) 당사자만 들여다볼 수 있다.
     */
    private fun validateParticipant(
        contract: Contract,
        member: Member
    ) {

        if (!contract.isParty(member.getId())) {

            throw ApplicationException(ContractStatusCode.CONTRACT_FORBIDDEN)
        }
    }

    /**
     * 시작·종료 시각은 둘 다 비워 둘 수 있지만, 둘 다 있으면 순서가 맞아야 한다.
     */
    private fun validatePeriod(
        startedAt: LocalDateTime?,
        endedAt: LocalDateTime?
    ) {

        if (startedAt == null || endedAt == null) {

            return
        }

        if (endedAt.isBefore(startedAt)) {

            throw ApplicationException(ContractStatusCode.CONTRACT_INVALID_PERIOD)
        }
    }

    private fun getMemberEntity(
        memberId: Long
    ): Member {

        return memberRepository.findMemberById(memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
    }

    private fun getCurrentMember(): Member {

        val username = SecurityContextHolder.getContext().authentication?.name

        if (username.isNullOrBlank() || username == "anonymousUser") {

            throw ApplicationException(AuthStatusCode.INVALID_JWT)
        }

        return memberRepository.findByUsername(username)
            ?: throw ApplicationException(AuthStatusCode.USERNAME_NOT_FOUND)
    }
}
