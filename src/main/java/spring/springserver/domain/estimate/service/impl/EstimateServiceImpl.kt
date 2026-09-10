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
import spring.springserver.domain.post.entity.Post
import spring.springserver.domain.post.exception.PostStatusCode
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.global.exception.exception.ApplicationException

@Service
@Transactional(rollbackFor = [Exception::class])
class EstimateServiceImpl(
    private val estimateRepository: EstimateRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository
): EstimateService {

    override fun createEstimate(
        createEstimateRequest: CreateEstimateRequest
    ): EstimateResponse {

        /**
         * 계약을 제안하는 쪽은 용역 판매자(을)이므로 발행자가 곧 전문가다.
         * 제안을 수락하고 대금을 지불하는 쪽이 의뢰인(갑)이다.
         */
        val professional = getCurrentMember()

        val client = memberRepository.findMemberById(createEstimateRequest.clientId!!)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

        if (client.getId() == professional.getId()) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_INVALID_MEMBER)
        }

        val estimate = estimateRepository.save(
            Estimate(
                post = getPostEntity(createEstimateRequest.postId!!),
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
    override fun getMyEstimates(
        postId: Long?
    ): List<EstimateResponse> {

        val member = getCurrentMember()

        val estimates = postId?.let {

            estimateRepository.findAllByPostAndParticipant(
                getPostEntity(it),
                member
            )
        }
            ?: estimateRepository.findAllByClientOrProfessional(
                member,
                member
            )

        return estimates.map { EstimateResponse.of(it) }
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

    @Transactional(readOnly = true)
    override fun isProfessional(
        estimateId: Long,
        memberId: Long
    ): Boolean {

        val estimate = estimateRepository.findById(estimateId).orElse(null)
            ?: return false

        return estimate.professional.getId() == memberId
    }

    private fun getPostEntity(
        postId: Long
    ): Post {

        return postRepository.findPostById(postId)
            ?: throw ApplicationException(PostStatusCode.INVALID_POST)
    }

    private fun getEstimateEntity(
        estimateId: Long
    ): Estimate {

        return estimateRepository.findById(estimateId).orElseThrow {

            ApplicationException(EstimateStatusCode.ESTIMATE_NOT_FOUND)
        }
    }

    /**
     * 결제가 완료된(PAID) 견적서는 조회만 가능하다.
     */
    private fun validateNotPaid(
        estimate: Estimate
    ) {

        if (estimate.isPaid()) {

            throw ApplicationException(EstimateStatusCode.ESTIMATE_ALREADY_PAID)
        }
    }

    /**
     * 결제 대금을 지불하는 쪽은 제안을 수락하는 의뢰인(갑)이다.
     * 발행 주체와 무관하게 결제 검증은 항상 의뢰인 기준으로 한다.
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

    /**
     * 제안한 쪽만 자기 제안을 고치거나 거둘 수 있다.
     * 수락하는 의뢰인이 금액을 바꿀 수 있으면 위변조 여지가 생긴다.
     */
    private fun validateOwner(
        estimate: Estimate,
        member: Member
    ) {

        if (estimate.professional.getId() != member.getId()) {

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
