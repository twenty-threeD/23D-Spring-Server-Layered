package spring.springserver.domain.auth.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import spring.springserver.domain.auth.entity.RefreshToken
import java.time.LocalDateTime

interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {

    fun findByToken(
        token: String
    ): RefreshToken?

    fun findByMemberId(
        memberId: Long
    ): RefreshToken?

    fun deleteByMemberId(
        memberId: Long
    )

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            DELETE FROM RefreshToken r
            WHERE r.expiresAt < :now
            """
    )
    fun deleteAllExpired(
        @Param("now") now: LocalDateTime
    ): Int
}
