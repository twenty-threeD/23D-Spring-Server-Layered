package spring.springserver.domain.member.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import spring.springserver.domain.member.entity.Member

interface MemberRepository: JpaRepository<Member, Long> {

    fun findByUsername(username: String): Member?

    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun existsByPhone(phone: String): Boolean

    @Query("SELECT m.username FROM Member m WHERE m.email = :email")
    fun findUsernameByEmail(email: String): String?

    fun email(email: String): String?
}