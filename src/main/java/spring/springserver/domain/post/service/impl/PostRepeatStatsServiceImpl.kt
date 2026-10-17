package spring.springserver.domain.post.service.impl

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring.springserver.domain.contract.repository.ContractRepository
import spring.springserver.domain.payment.entity.PaymentStatus
import spring.springserver.domain.post.repository.PostRepository
import spring.springserver.domain.post.service.PostRepeatStats
import spring.springserver.domain.post.service.PostRepeatStatsService

@Service
@Transactional(rollbackFor = [Exception::class])
class PostRepeatStatsServiceImpl(
    private val postRepository: PostRepository,
    private val contractRepository: ContractRepository,
    private val entityManager: EntityManager
): PostRepeatStatsService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun recompute(
        postId: Long
    ) {

        val post = postRepository.findPostById(postId)
            ?: return

        val authorId = post.member.getId()
            ?: return

        val stats = PostRepeatStats.of(
            aggregateCounts(
                postId,
                authorId
            )
        )

        post.applyRepeatStats(
            stats.buyerCount,
            stats.repeatBuyerCount,
            stats.repeatRate
        )
    }

    override fun recomputeByContractId(
        contractId: Long?
    ) {

        if (contractId == null) {

            return
        }

        val postId = contractRepository.findContractById(contractId)
            ?.post
            ?.getId()
            ?: return

        recompute(postId)
    }

    override fun recomputeAll(): Int {

        val postIds = postRepository.findAllPostIdsOfLivingPosts()

        postIds.forEach { postId -> recompute(postId) }

        log.info("재구매율 백필 완료. 대상 post 수 = {}", postIds.size)

        return postIds.size
    }

    /**
     * 결제가 완료된 Payment 를 갑(client) 별로 묶어 건수를 돌려준다.
     * Payment.contractId 가 FK 가 아니라 Long 이므로 Contract 를 수동으로 조인한다.
     * 작성자 본인이 자기 글로 체결한 계약은 제외한다.
     */
    @Suppress("UNCHECKED_CAST")
    private fun aggregateCounts(
        postId: Long,
        authorId: Long
    ): List<Long> {

        val query = entityManager.createQuery(
            """
            select count(distinct ct.id)
            from Payment pm, Contract ct
            where pm.contractId = ct.id
              and ct.post.id = :postId
              and ct.client.id <> :authorId
              and pm.status in :completedStatuses
            group by ct.client.id
            """
        )
        query.setParameter("postId", postId)
        query.setParameter("authorId", authorId)
        query.setParameter("completedStatuses", COMPLETED_STATUSES)

        return (query.resultList as List<Long>)
    }

    companion object {

        private val COMPLETED_STATUSES = listOf(
            PaymentStatus.DONE,
            PaymentStatus.CHAIN_RECORDED
        )
    }
}
