package spring.springserver.domain.member.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.member.entity.Member
import java.time.LocalDateTime

interface MemberRepository: JpaRepository<Member, Long> {

    fun findByUsername(
        username: String?
    ): Member?
    fun findByEmail(
        email: String
    ): Member?

    fun findByPhone(
        phone: String
    ): Member?

    fun existsByUsername(
        username: String
    ): Boolean
    fun existsByEmail(
        email: String
    ): Boolean
    fun existsByPhone(
        phone: String
    ): Boolean

    @Query(
        """
            SELECT m.username 
            FROM Member m 
            WHERE m.email = :email
              AND m.deletedAt IS NULL
            """
    )
    fun findUsernameByEmail(
        email: String
    ): String?

    /**
     * 알림 팬아웃처럼 수신자가 여러 명인 경로에서 쓴다.
     * 수신자마다 `findByUsername`을 호출하면 조회가 수신자 수에 비례해 늘어난다.
     */
    @Query(
        """
            select m.username, m.id
            from Member m
            where m.username in :usernames
            """
    )
    fun findIdsByUsernameIn(
        @Param("usernames") usernames: Collection<String>
    ): List<Array<Any?>>
    /**
     * 재가입 제한 기간이 지났는데 아직 익명화되지 않은 탈퇴 회원.
     */
    fun findAllByDeletedAtBeforeAndAnonymizedAtIsNull(
        deletedAt: LocalDateTime
    ): List<Member>

    fun findMemberById(
        id: Long
    ): Member?
}