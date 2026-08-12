package spring.springserver.domain.estimate.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.estimate.entity.Estimate
import spring.springserver.domain.member.entity.Member

interface EstimateRepository: JpaRepository<Estimate, Long> {

    fun findAllByClient(
        client: Member
    ): List<Estimate>

    fun findAllByProfessional(
        professional: Member
    ): List<Estimate>

    fun findAllByClientOrProfessional(
        client: Member,
        professional: Member
    ): List<Estimate>
}
