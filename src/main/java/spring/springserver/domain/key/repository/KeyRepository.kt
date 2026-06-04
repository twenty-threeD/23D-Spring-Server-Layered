package spring.springserver.domain.key.repository

import org.springframework.data.jpa.repository.JpaRepository
import spring.springserver.domain.key.entity.MemberKey

interface KeyRepository: JpaRepository<MemberKey, Long> {
}