package spring.springserver.domain.key.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import spring.springserver.domain.key.entity.MemberKey
import spring.springserver.domain.member.entity.Member

interface KeyRepository: JpaRepository<MemberKey, Long> {

    fun findByMemberId(memberId: Long): MemberKey?
    fun member(member: Member): MutableList<MemberKey>

    @Query("select k from MemberKey k where k.privateKey not like 'v1:%'")
    fun findAllPlaintext(): List<MemberKey>
}