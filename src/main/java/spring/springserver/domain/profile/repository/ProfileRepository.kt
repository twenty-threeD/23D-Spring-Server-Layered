package spring.springserver.domain.profile.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.member.entity.Member
import spring.springserver.domain.profile.entity.Profile

interface ProfileRepository: JpaRepository<Profile, Long> {

    fun findByMember(
        member: Member
    ): Profile?

    fun existsByMember(
        member: Member
    ): Boolean
}