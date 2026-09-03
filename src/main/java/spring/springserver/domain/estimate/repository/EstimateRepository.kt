package spring.springserver.domain.estimate.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.estimate.entity.Estimate
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.post.entity.Post

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

    /**
     * 게시글 단위로 좁혀 조회한다.
     * 파생 쿼리로는 or 조건의 우선순위가 모호해지므로 직접 작성한다.
     */
    @Query(
        "select estimate from Estimate estimate " +
                "where estimate.post = :post " +
                "and (estimate.client = :member or estimate.professional = :member)"
    )
    fun findAllByPostAndParticipant(
        @Param("post") post: Post,
        @Param("member") member: Member
    ): List<Estimate>
}
