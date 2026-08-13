package spring.springserver.domain.estimate.service.impl

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.auth.exception.AuthStatusCode
import spring.springserver.domain.estimate.data.request.CreateEstimateRequest
import spring.springserver.domain.estimate.data.request.UpdateEstimateRequest
import spring.springserver.domain.estimate.data.response.DeleteEstimateResponse
import spring.springserver.domain.estimate.data.response.EstimateResponse
import spring.springserver.domain.estimate.entity.Estimate
import spring.springserver.domain.estimate.exception.EstimateStatusCode
import spring.springserver.domain.estimate.repository.EstimateRepository
import spring.springserver.domain.estimate.service.EstimateService
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.member.exception.MemberStatusCode
import spring.springserver.domain.member.repository.MemberRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class EstimateServiceImpl(
    private val estimateRepository: EstimateRepository,
    private val memberRepository: MemberRepository
): EstimateService {

    override fun createEstimate(
        createEstimateRequest: CreateEstimateRequest
    ): EstimateResponse {

        val client = getCurrentMember()

        val professional = memberRepository.findMemberById(createEstimateRequest.professionalId!!)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (client.getId() == professional.getId()) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_INVALID_MEMBER)
        }

        val estimate = estimateRepository.save(
            Estimate(
                client = client,
                professional = professional,
                url = createEstimateRequest.url!!.trim(),
                totalPay = createEstimateRequest.totalPay!!
            )
        )

        return EstimateResponse.of(estimate)
    }

    @Transactional(readOnly = true)
    override fun getEstimate(
        estimateId: Long
    ): EstimateResponse {

        val member = getCurrentMember()
        val estimate = getEstimateEntity(estimateId)

        validateParticipant(estimate, member)

        return EstimateResponse.of(estimate)
    }

    @Transactional(readOnly = true)
    override fun getMyEstimates(): List<EstimateResponse> {

        val member = getCurrentMember()

        return estimateRepository.findAllByClientOrProfessional(member, member)
            .map { EstimateResponse.of(it) }
    }

    override fun updateEstimate(
        estimateId: Long,
        updateEstimateRequest: UpdateEstimateRequest
    ): EstimateResponse {

        val member = getCurrentMember()
        val estimate = getEstimateEntity(estimateId)

        validateOwner(estimate, member)
        validateNotPaid(estimate)

        estimate.update(
            updateEstimateRequest.url!!.trim(),
            updateEstimateRequest.totalPay!!
        )

        return EstimateResponse.of(estimate)
    }

    override fun deleteEstimate(
        estimateId: Long
    ): DeleteEstimateResponse {

        val member = getCurrentMember()
        val estimate = getEstimateEntity(estimateId)

        validateOwner(estimate, member)
        validateNotPaid(estimate)

        estimateRepository.delete(estimate)

        return DeleteEstimateResponse.of("견적서가 삭제되었습니다.")
    }

    @Transactional(readOnly = true)
    override fun validatePayable(
        estimateId: Long,
        memberId: Long,
        amount: Long
    ) {

        val estimate = getEstimateEntity(estimateId)

        validateClient(estimate, memberId)
        validateNotPaid(estimate)
        validateAmount(estimate, amount)
    }

    override fun markAsPaid(
        estimateId: Long,
        memberId: Long,
        paidAmount: Long
    ): EstimateResponse {

        val estimate = getEstimateEntity(estimateId)

        validateClient(estimate, memberId)
        validateNotPaid(estimate)
        validateAmount(estimate, paidAmount)

        estimate.markAsPaid()

        return EstimateResponse.of(estimate)
    }

    private fun getEstimateEntity(
        estimateId: Long
    ): Estimate {

        return estimateRepository.findById(estimateId).orElseThrow {

            ApplicationException(EstimateStatusCode.ESTIMATE_NOT_FOUND)
        }
    }

    /**
     * 결제가 완료(paid = true)된 견적서는 조회만 가능하다.
     */
    private fun validateNotPaid(
        estimate: Estimate
    ) {

        if (estimate.paid) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_ALREADY_PAID)
        }
    }

    /**
     * 견적서의 작성·수정·삭제 권한은 일을 맡기는 클라이언트에게 있다.
     * 전문가는 자신에게 온 견적서를 조회만 할 수 있다.
     */
    private fun validateClient(
        estimate: Estimate,
        memberId: Long
    ) {

        if (estimate.client.getId() != memberId) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_FORBIDDEN)
        }
    }

    /**
     * 결제 금액이 견적서의 최종 금액과 다르면 결제를 인정하지 않는다.
     */
    private fun validateAmount(
        estimate: Estimate,
        amount: Long
    ) {

        if (estimate.totalPay != amount) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_AMOUNT_MISMATCH)
        }
    }

    private fun validateOwner(
        estimate: Estimate,
        member: Member
    ) {

        if (estimate.client.getId() != member.getId()) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_FORBIDDEN)
        }
    }

    private fun validateParticipant(
        estimate: Estimate,
        member: Member
    ) {

        if (estimate.client.getId() != member.getId() &&
            estimate.professional.getId() != member.getId()) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_FORBIDDEN)
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
}
