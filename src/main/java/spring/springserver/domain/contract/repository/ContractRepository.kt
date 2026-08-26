package spring.springserver.domain.contract.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.contract.entity.Contract
import spring.springserver.domain.member.entity.Member

interface ContractRepository: JpaRepository<Contract, Long> {

    /**
     * 로그인한 회원이 갑이거나 을인 계약서를 모두 찾는다.
     * 응답을 만들 때 당사자를 모두 읽으므로 함께 가져온다.
     */
    @Query(
        "select contract from Contract contract " +
                "join fetch contract.partA contractPartA " +
                "join fetch contract.partB contractPartB " +
                "join fetch contract.writer " +
                "where contractPartA = :member or contractPartB = :member"
    )
    fun findAllByParticipant(
        @Param("member") member: Member
    ): List<Contract>
}
