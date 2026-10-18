package spring.springserver.domain.push.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.push.entity.PushToken

interface PushTokenRepository : JpaRepository<PushToken, Long> {

    fun findByToken(
        token: String
    ): PushToken?

    @Query(
        """
        select p.token
        from PushToken p
        where p.member.username = :username
        """
    )
    fun findTokensByUsername(
        @Param("username") username: String
    ): List<String>

    @Modifying
    @Query(
        """
        delete from PushToken p
        where p.token = :token
          and p.member.id = :memberId
        """
    )
    fun deleteByTokenAndMemberId(
        @Param("token") token: String,
        @Param("memberId") memberId: Long
    ): Int

    @Modifying
    @Query(
        """
        delete from PushToken p
        where p.token in :tokens
        """
    )
    fun deleteAllByTokenIn(
        @Param("tokens") tokens: Collection<String>
    ): Int
}
